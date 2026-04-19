package com.autric.upbit.domain.chart.sync;

import com.autric.upbit.domain.chart.entity.ChartSyncMeta;
import com.autric.upbit.domain.chart.entity.Market;
import com.autric.upbit.domain.chart.repository.ChartSyncMetaRepository;
import com.autric.upbit.external.upbit.client.UpbitApiClient;
import com.autric.upbit.external.upbit.dto.response.UpbitCandleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 차트 전체(Full) 동기화를 수행하는 실행 컴포넌트.
 *
 * <p>지정된 마켓과 캔들 단위(unit)에 대해 최신 시점부터 과거 방향으로 차트를 조회하여
 * 최대 지정 건수만큼 DB에 저장하며, 동기화 메타 정보를 갱신합니다.
 *
 * @see UpbitApiClient
 * @see ChartPersistHelper
 * @see ChartSyncMeta
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChartFullSyncExecutor {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UpbitApiClient upbitApiClient;
    private final ChartPersistHelper persistHelper;
    private final ChartSyncMetaRepository chartSyncMetaRepository;

    @Transactional
    public int execute(ChartSyncMeta syncMeta, Market market, int unit) {
        final int count = 200;
        final int maxSyncCount = 1000;
        int totalSyncedCount = 0;

        LocalDateTime toTime = LocalDateTime.now(KST);
        LocalDateTime fullSyncedLatestTime = null;

        while (totalSyncedCount < maxSyncCount) {

            List<UpbitCandleResponse> responseList = (unit == 1440)
                    ? upbitApiClient.getDayCandles(market, count, toTime)
                    : upbitApiClient.getMinuteCandles(market, unit, count, toTime);

            if (responseList.isEmpty()) {
                break;
            }

            LocalDateTime latestCandleTime = responseList.get(0).getParsedDateTime();
            LocalDateTime oldestCandleTime = responseList.get(responseList.size() - 1).getParsedDateTime();

            totalSyncedCount += persistHelper.persistByUnit(responseList, market, unit);


            if (fullSyncedLatestTime == null) {
                fullSyncedLatestTime = latestCandleTime;
            }

            toTime = oldestCandleTime.minusSeconds(1);

            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (fullSyncedLatestTime != null) {
            syncMeta.updateLastSyncedAt(fullSyncedLatestTime);
            syncMeta.markFullSynced();
            chartSyncMetaRepository.save(syncMeta);
        }

        return totalSyncedCount;
    }
}
