package cn.iocoder.mall.order.biz.service;

import cn.iocoder.mall.order.api.OrderCommentService;
import cn.iocoder.mall.order.api.bo.OrderCommentCreateBO;
import cn.iocoder.mall.order.api.bo.OrderCommentInfoBO;
import cn.iocoder.mall.order.api.bo.OrderCommentPageBO;
import cn.iocoder.mall.order.api.constant.OrderReplyUserTypeEnum;
import cn.iocoder.mall.order.api.dto.OrderCommentCreateDTO;
import cn.iocoder.mall.order.api.dto.OrderCommentPageDTO;
import cn.iocoder.mall.order.biz.convert.OrderCommentConvert;
import cn.iocoder.mall.order.biz.dao.OrderCommentMapper;
import cn.iocoder.mall.order.biz.dao.OrderCommentReplayMapper;
import cn.iocoder.mall.order.biz.dataobject.OrderCommentDO;
import cn.iocoder.mall.order.biz.dataobject.OrderCommentReplyDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * 订单评论 service impl
 *
 * @author wtz
 * @time 2019
 */
@Service
@org.apache.dubbo.config.annotation.Service(validation = "true", version = "${dubbo.provider.OrderCommentService.version}")
public class OrderCommentServiceImpl implements OrderCommentService {

    @Autowired
    private OrderCommentMapper orderCommentMapper;

    @Autowired
    private OrderCommentReplayMapper orderCommentReplayMapper;


    @Autowired
    private OrderCommentService orderCommentService;

    @Override
    public OrderCommentCreateBO createOrderComment(OrderCommentCreateDTO orderCommentCreateDTO) {
        //首先判断订单状态是否处于待评价状态

        //接下来就是入库
        OrderCommentDO orderCommentDO=OrderCommentConvert.INSTANCE.convert(orderCommentCreateDTO);
        orderCommentDO.setCreateTime(new Date());
        orderCommentDO.setUpdateTime(new Date());
        orderCommentMapper.insert(orderCommentDO);
        return OrderCommentConvert.INSTANCE.convert(orderCommentDO);
    }

    @Override
    public OrderCommentPageBO getOrderCommentPage(OrderCommentPageDTO orderCommentPageDTO) {
        OrderCommentPageBO orderCommentPageBO=new OrderCommentPageBO();
        //分页内容
        List<OrderCommentDO> orderCommentDOList=orderCommentMapper.selectCommentPage(orderCommentPageDTO);
        //分页评论的 id
        List<Integer> commentIds=orderCommentDOList.stream().map(x->x.getId()).collect(Collectors.toList());
        //获取商家最新的评论回复
        List<OrderCommentReplyDO> orderCommentReplyDOList=orderCommentReplayMapper.selectCommentNewMerchantReplyByCommentIds(commentIds,
                OrderReplyUserTypeEnum.MERCHANT.getValue());
        //评论组装
        List<OrderCommentPageBO.OrderCommentItem> orderCommentItemList=orderCommentDOList.stream()
                .flatMap(x->orderCommentReplyDOList.stream()
                        .filter(y->x.getId()==y.getCommentId())
                        .map(y->new OrderCommentPageBO.OrderCommentItem(x.getId(),x.getUserAvatar(),x.getUserNickName(),x.getStar(),
                                x.getCommentContent(),x.getCommentPics(),x.getReplayCount(),x.getLikeCount(),x.getCreateTime(),y.getReplyContent()))
                ).collect(Collectors.toList());
        //总数
        int totalCount=orderCommentMapper.selectCommentTotalCountByProductSkuId(orderCommentPageDTO.getProductSkuId());
        orderCommentPageBO.setOrderCommentItems(orderCommentItemList);
        orderCommentPageBO.setTotal(totalCount);
        return orderCommentPageBO;
    }


    @Override
    public OrderCommentInfoBO getOrderCommentInfo(Integer commentId) {
        //查询评论详情
        OrderCommentDO orderCommentDO=orderCommentMapper.selectCommentInfoByCommentId(commentId);
        return OrderCommentConvert.INSTANCE.convertOrderCommentInfoBO(orderCommentDO);
    }

    @Override
    public Boolean OrderCommentTimeOutProductCommentTask() {
        return null;
    }
}
