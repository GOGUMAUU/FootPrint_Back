package com.example.footprint.service;

import com.example.footprint.dto.CommonDtos.PageResponse;
import com.example.footprint.entity.Block;
import com.example.footprint.entity.Follow;
import com.example.footprint.entity.User;
import com.example.footprint.exception.ApiException;
import com.example.footprint.repository.BlockRepository;
import com.example.footprint.repository.FollowRepository;
import com.example.footprint.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.footprint.dto.UserDtos.*;

@Service @RequiredArgsConstructor
public class RelationshipService {
    private final UserRepository users;
    private final FollowRepository follows;
    private final BlockRepository blocks;
    private final UserService profiles;
    private final Clock clock;
    private final EntityManager em;
    public record CreatedFollow(FollowResult result, boolean created) {}
    private void lockPair(Long a,Long b) {
        if(a.equals(b)) throw ApiException.bad("SELF_RELATION","자기 자신에게 요청할 수 없습니다.");
        for(Long id:List.of(Math.min(a,b),Math.max(a,b))) {
            var user=users.lockActive(id).orElseThrow(ApiException::missing);
            em.refresh(user);
            if(user.getDeletedAt()!=null) throw ApiException.missing();
        }
    }
    @Transactional
    public CreatedFollow follow(Long viewer,Long target) {
        lockPair(viewer,target);
        if(blocks.between(viewer,target)) throw ApiException.missing();
        var existing=follows.findByFollower_UserIdAndFollowing_UserId(viewer,target);
        if(existing.isPresent() && !"REJECTED".equals(existing.get().getStatus()))
            return new CreatedFollow(result(existing.get()),false);
        var from=profiles.active(viewer); var to=profiles.active(target);
        var f=existing.orElseGet(() -> Follow.builder().follower(from).following(to).build());
        f.setStatus(to.isPrivateAccount()?"PENDING":"ACCEPTED"); f.setRespondedAt(to.isPrivateAccount()?null:LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));
        if(existing.isPresent()) f.setCreatedAt(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));
        follows.saveAndFlush(f);
        if("ACCEPTED".equals(f.getStatus())) counts(f,1);
        return new CreatedFollow(result(f),existing.isEmpty());
    }
    @Transactional
    public void unfollow(Long viewer,Long target) {
        lockPair(viewer,target);
        follows.findByFollower_UserIdAndFollowing_UserId(viewer,target).ifPresent(this::remove);
    }
    @Transactional
    public FollowResult respond(Long viewer,Long followId,boolean accept) {
        // Read only scalar IDs before taking the canonical user locks; reload the relationship afterwards.
        var snapshot=follows.findById(followId).orElseThrow(ApiException::missing);
        Long from=snapshot.getFollower().getUserId(),to=snapshot.getFollowing().getUserId();
        if(!viewer.equals(to)) throw ApiException.forbidden();
        em.detach(snapshot);
        lockPair(from,to);
        var f=follows.findById(followId).orElseThrow(ApiException::missing);
        if(blocks.between(from,to)) throw ApiException.missing();
        String next=accept?"ACCEPTED":"REJECTED";
        if(next.equals(f.getStatus())) return result(f);
        if(!"PENDING".equals(f.getStatus())) throw ApiException.conflict("FOLLOW_STATE_CONFLICT");
        f.setStatus(next); f.setRespondedAt(LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));
        if(accept) counts(f,1);
        return result(f);
    }
    @Transactional
    public void block(Long viewer,Long target) {
        lockPair(viewer,target);
        if(blocks.existsByBlockerIdAndBlockedId(viewer,target)) return;
        var b=new Block(); b.setBlockerId(viewer); b.setBlockedId(target); b.setCreatedAt(clock.instant()); blocks.save(b);
        follows.findByFollower_UserIdAndFollowing_UserId(viewer,target).ifPresent(this::remove);
        follows.findByFollower_UserIdAndFollowing_UserId(target,viewer).ifPresent(this::remove);
    }
    @Transactional
    public void unblock(Long viewer,Long target) {
        lockPair(viewer,target);
        blocks.findByBlockerIdAndBlockedId(viewer,target).ifPresent(blocks::delete);
    }
    public void remove(Follow f) { if("ACCEPTED".equals(f.getStatus())) counts(f,-1); follows.delete(f); }
    private void counts(Follow f,int delta) {
        User from=f.getFollower(),to=f.getFollowing();
        from.setFollowingCount(Math.max(0,UserService.nz(from.getFollowingCount())+delta));
        to.setFollowerCount(Math.max(0,UserService.nz(to.getFollowerCount())+delta));
    }
    private FollowResult result(Follow f) { return new FollowResult(f.getFollowId(),f.getStatus()); }
    @Transactional(readOnly=true)
    public PageResponse<Summary> list(Long viewer,Long target,boolean followers,int page,int size) {
        profiles.requireContentAccess(viewer,target);
        var slice=followers?follows.followers(target,viewer,PageResponse.request(page,size)):follows.following(target,viewer,PageResponse.request(page,size));
        return PageResponse.from(slice,f -> profiles.summary(viewer,followers?f.getFollower():f.getFollowing()));
    }
    @Transactional(readOnly=true)
    public PageResponse<FollowRequest> requests(Long viewer,int page,int size) {
        return PageResponse.from(follows.findByFollowing_UserIdAndStatusOrderByCreatedAtDescFollowIdDesc(viewer,"PENDING",PageResponse.request(page,size)),
            f -> new FollowRequest(f.getFollowId(),profiles.summary(viewer,f.getFollower()),f.getCreatedAt()));
    }
    public record BlockItem(Summary user,Instant createdAt) {}
    @Transactional(readOnly=true)
    public PageResponse<BlockItem> blockList(Long viewer,int page,int size) {
        return PageResponse.from(blocks.findByBlockerIdOrderByCreatedAtDescBlockIdDesc(viewer,PageResponse.request(page,size)),
            b -> new BlockItem(profiles.summary(viewer,profiles.active(b.getBlockedId())),b.getCreatedAt()));
    }
}
