package com.volunteer.service;

import com.volunteer.entity.HourTransfer;
import java.util.List;
import java.util.Map;

public interface HourTransferService {
    void transferHours(Integer fromUserId, Integer toUserId, Integer hours, String reason) throws Exception;
    void respondToTransfer(Integer transferId, Integer status) throws Exception;
    List<HourTransfer> getTransferRecords(Map<String, Object> params);
    int countTransferRecords(Map<String, Object> params);
}