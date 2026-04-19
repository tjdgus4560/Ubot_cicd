package com.autric.upbit.domain.account.scheduler;

import com.autric.upbit.domain.account.service.AccountService;
import com.autric.upbit.domain.member.entity.Member;
import com.autric.upbit.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 일별 자산 스냅샷 스케줄러
 * 매일 08:50에 실행 (업비트 기준 09:00이 하루 시작)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountHistoryScheduler {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AccountService accountService;
    private final MemberRepository memberRepository;

    /**
     * 매일 08:50에 전체 회원의 자산 스냅샷 저장
     * 
     * - 업비트 기준: 09:00이 새로운 하루 시작
     * - 08:50에 전일(어제) 기준 스냅샷 저장
     */
    @Scheduled(cron = "0 50 8 * * *", zone = "Asia/Seoul")
    public void saveAllMemberSnapshots() {
        log.info("AccountHistoryScheduler 시작 → 전체 회원 스냅샷 저장");

        // 스냅샷 날짜는 전일 (업비트 기준 어제 09:00 ~ 오늘 08:59 범위)
        LocalDate snapshotDate = LocalDate.now(KST).minusDays(1);

        // API 키가 등록된 회원만 조회
        List<Member> members = memberRepository.findAll().stream()
                .filter(Member::hasApiKey)
                .toList();

        log.info("스냅샷 대상 회원 수: {}", members.size());

        int successCount = 0;
        int failCount = 0;

        for (Member member : members) {
            try {
                accountService.saveSnapshot(member, snapshotDate);
                successCount++;
                
                // API 요청 간 딜레이 (Rate Limit 대응)
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("스냅샷 저장 실패: memberId={}, error={}", member.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("AccountHistoryScheduler 완료 → 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 수동 스냅샷 저장 (테스트/복구용)
     */
    public void saveSnapshotManually(LocalDate date) {
        log.info("수동 스냅샷 저장 시작: date={}", date);

        List<Member> members = memberRepository.findAll().stream()
                .filter(Member::hasApiKey)
                .toList();

        for (Member member : members) {
            try {
                accountService.saveSnapshot(member, date);
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("수동 스냅샷 저장 실패: memberId={}, date={}, error={}", 
                        member.getId(), date, e.getMessage());
            }
        }

        log.info("수동 스냅샷 저장 완료: date={}", date);
    }
}
