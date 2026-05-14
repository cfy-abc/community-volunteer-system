package com.volunteer.service.impl;

import com.volunteer.entity.HourTransfer;
import com.volunteer.entity.User;
import com.volunteer.mapper.HourTransferMapper;
import com.volunteer.mapper.UserMapper;
import com.volunteer.service.HourTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class HourTransferServiceImpl implements HourTransferService {

    @Autowired
    private HourTransferMapper transferMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void transferHours(Integer fromUserId, Integer toUserId, Integer hours, String reason) throws Exception {
        // 检查用户是否存在
        User fromUser = userMapper.findById(fromUserId);
        User toUser = userMapper.findById(toUserId);

        if (fromUser == null || toUser == null) {
            throw new Exception("用户不存在");
        }

        if (fromUser.getStatus() != 1 || toUser.getStatus() != 1) {
            throw new Exception("用户未通过审核");
        }

        // 检查时长是否足够
        if (fromUser.getVolunteerHours() < hours) {
            throw new Exception("您的可用时长不足");
        }

        // 创建转账记录
        HourTransfer transfer = new HourTransfer();
        transfer.setFromUserId(fromUserId);
        transfer.setToUserId(toUserId);
        transfer.setHours(hours);
        transfer.setReason(reason);
        transfer.setStatus(0); // 0表示待处理

        transferMapper.insert(transfer);
    }

    @Override
    @Transactional
    public void respondToTransfer(Integer transferId, Integer status) throws Exception {
        // 检查记录是否存在
        HourTransfer transfer = transferMapper.findById(transferId);
        if (transfer == null) {
            throw new Exception("转账记录不存在");
        }

        if (transfer.getStatus() != 0) {
            throw new Exception("该转账记录已处理");
        }

        // 更新状态
        transferMapper.updateStatus(transferId, status);

        // 如果同意，更新用户时长
        if (status == 1) {
            User fromUser = userMapper.findById(transfer.getFromUserId());
            User toUser = userMapper.findById(transfer.getToUserId());

            if (fromUser != null && toUser != null) {
                // 扣减发起方时长
                fromUser.setVolunteerHours(fromUser.getVolunteerHours() - transfer.getHours());
                fromUser.setTotalSpentHours(fromUser.getTotalSpentHours() + transfer.getHours());
                userMapper.updateHours(fromUser);

                // 增加接收方时长
                toUser.setVolunteerHours(toUser.getVolunteerHours() + transfer.getHours());
                toUser.setTotalEarnedHours(toUser.getTotalEarnedHours() + transfer.getHours());
                userMapper.updateHours(toUser);
            }
        }
    }

    @Override
    public List<HourTransfer> getTransferRecords(Map<String, Object> params) {
        return transferMapper.findPage(params);
    }

    @Override
    public int countTransferRecords(Map<String, Object> params) {
        return transferMapper.count(params);
    }
}