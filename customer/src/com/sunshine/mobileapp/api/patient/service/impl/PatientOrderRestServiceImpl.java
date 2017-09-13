/**
 * <html>
 * <body>
 *  <P> Copyright 2014 广东天泽阳光康众医疗投资管理有限公司. 粤ICP备09007530号-15</p>
 *  <p> All rights reserved.</p>
 *  <p> Created on 2017年9月6日</p>
 *  <p> Created by 钟乳石</p>
 *  </body>
 * </html>
 */
package com.sunshine.mobileapp.api.patient.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import com.sunshine.cache.component.DoctorAvailableAppointmentTimeCache;
import com.sunshine.cache.component.SysServiceCache;
import com.sunshine.cache.component.UserAliasNameCache;
import com.sunshine.common.GlobalConstant;
import com.sunshine.common.MessageCentorConstant;
import com.sunshine.common.exception.IllegalDateException;
import com.sunshine.common.exception.OrderException;
import com.sunshine.framework.utils.DateUtils;
import com.sunshine.mobileapp.api.RestResponse;
import com.sunshine.mobileapp.api.RestStatusEnum;
import com.sunshine.mobileapp.api.index.doctorscollect.dao.DoctorUserDao;
import com.sunshine.mobileapp.api.index.doctorscollect.entity.DoctorUser;
import com.sunshine.mobileapp.api.order.entity.Order;
import com.sunshine.mobileapp.api.order.planning.AvailableTime;
import com.sunshine.mobileapp.api.order.service.PatientOrderService;
import com.sunshine.mobileapp.api.order.vo.OrderStatusEnum;
import com.sunshine.mobileapp.api.patient.dao.InquireUserDao;
import com.sunshine.mobileapp.api.patient.dao.PatientUserDao;
import com.sunshine.mobileapp.api.patient.entity.PatientUser;
import com.sunshine.mobileapp.api.patient.service.PatientOrderRestService;

/**
* @Package：com.sunshine.mobileapp.api.patient.service.impl   
* @ClassName：InquireUserRestServiceImpl   
* @Description：   <p> 问诊人相关操作接口</p>
* @Author： - DaoDou 
* @CreatTime：2017年9月10日 下午4:08:56   
* @Modify By：   
* @ModifyTime：  
* @Modify marker：   
* @version    V1.0
 */
@Path("v1/patientOrder")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
@Produces({ ContentType.APPLICATION_JSON_UTF_8, ContentType.TEXT_XML_UTF_8 })
public class PatientOrderRestServiceImpl implements PatientOrderRestService{
	
	private static final Logger logger = LoggerFactory.getLogger(PatientOrderRestServiceImpl.class);
	
	@Autowired
	private PatientOrderService orderService;
	
	/*@Autowired
	private PatientOrderDao orderDao;*/
	
	@Autowired
	private InquireUserDao inquireUserDao;

	@Autowired
	private DoctorUserDao doctorUserDao;
	
	@Autowired
	private PatientUserDao patientUserDao;
	
	@Autowired
	private UserAliasNameCache userAliasNameCache;
	
	@Autowired
	private DoctorAvailableAppointmentTimeCache doctorAvailableAppointmentTimeCache;
	
	@Autowired
	private SysServiceCache sysServiceCache;

	@Override
	public RestResponse generateOrder(String account, String doctor,
			String serviceId, String serviceName, String appointmentDate,
			String appointmentTime, Integer buyTime, String imageKeys) {
		
		RestResponse response = null;
		List<String> testUserSet = new ArrayList<String>();
		testUserSet.add("陆晓文");
		testUserSet.add("程前");
		testUserSet.add("林丰");
		testUserSet.add("张果");
		testUserSet.add("程采夫");
		testUserSet.add("林义一");
		
		String userName = testUserSet.get(Math.abs(account.hashCode()) % 6);
		DoctorUser doctorUser = null;
		PatientUser user = null;
		String alias = null;
		try {
			// 电话咨询服务ID
			doctorUser = doctorUserDao.findByAccount(doctor);
			user = patientUserDao.findByAccount(account);
			
			if (user == null) {
				user = new PatientUser();
				user.setUserName(userName);
				user.setAccount(account);
				user.setAge(20);
				user.setSex(1);
				user.setHeight(170);
				user.setWeight(60);
				String id = patientUserDao.insert(user);
				user.setId(id);
			} else {
				alias = userAliasNameCache.getUserAliasNameCache(doctorUser.getId(), user.getId());
			}
			if (doctorUser == null) {
				throw new IllegalDateException("不存在医生[" + doctor + "]");
			}
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
			Order order = new Order();
			order.setUserId(user.getId());
			order.setUserName(alias == null ? user.getUserName() : alias);
			order.setUserPhone(user.getAccount());
			
			order.setDoctorId(doctorUser.getId());
			order.setDoctorName(doctorUser.getUserName());
			// for test
			if ("admin".equals(doctorUser.getAccount())) {
				order.setDoctorPhone("18026281027");
			} else {
				order.setDoctorPhone(doctorUser.getAccount());
			}
			if (GlobalConstant.SERVICE_TYPE_PHONE_CALL == sysServiceCache.getConsultServiceType(serviceId)) {
				order.setServiceMsgCode(MessageCentorConstant.YS_DHZX_MESSAGE_CODE);
			} else {
				order.setServiceMsgCode(MessageCentorConstant.YS_TWZX_MESSAGE_CODE);
			}
			
			order.setServiceId(serviceId);
			
			order.setAppointmentTime(f.parse(appointmentDate));
			order.setAppointmentTimeStr(appointmentTime);
			order.setBuyTime(buyTime != null ? buyTime : 0);
			
			String smsMsg = "尊敬的医生大人，您有新的";
			// 需要增加一个服务类型标识区分电话，图文，视频
			// 电话咨询服务
			if (buyTime != null && buyTime > 0) {
				order.setOrderDesc("电话咨询服务订单");
				smsMsg += "电话咨询";
			} else {// 图文咨询服务
				order.setOrderDesc("图文咨询服务订单");
				smsMsg += "图文咨询";
			}
			smsMsg += "会诊申请，快打开易健通APP接诊吧！";
			// 待接诊
			order.setOrderStatus(OrderStatusEnum.UNRECEIVE.getOrderStatus());
			
			order.setProvinceCode("440000");
			order.setProvinceName("广东省");
			order.setCityCode("440100");
			order.setCityName("广州市");
			order.setAreaCode("440106");
			order.setAreaName("天河区");
			order.setImageUrls(imageKeys);
			
			order = orderService.generateOrder(order, appointmentDate, appointmentTime);
			
			//resp = new RestResponse(Status.OK, "生成订单成功");
			//resp.setResult(order);
//			MsgPushService msgPushService = SpringContextHolder.getBean(MsgPushService.class);
//			msgPushService.captchaPush(order.getDoctorPhone(), smsMsg);
			
		} catch(IllegalDateException e) {
			response = new RestResponse(RestStatusEnum.ERROR, e.getMessage());
			if (e.getCode() != -1) {
				logger.error("生成订单发生异常", e);
			}
		} catch(OrderException e) {
			response = new RestResponse(RestStatusEnum.ERROR, e.getMessage());
			logger.error("生成订单发生异常", e);
		} catch(Exception e) {
			response = new RestResponse(RestStatusEnum.ERROR, "生成订单发生异常");
			logger.error("生成订单发生异常", e);
		}
		
		// for test use temporarily begin
		List<AvailableTime> timeList = null;
		if (doctorUser != null && appointmentDate != null) {
			timeList = doctorAvailableAppointmentTimeCache.getAvailableAppointmentTime(DateUtils.StringToDate(appointmentDate), doctorUser.getId());
		}
		StringBuilder leftAppointTime = new StringBuilder("医生剩余预约时间：");
		
		if (timeList != null && timeList.size() > 0) {
			List<AvailableTime> list = new ArrayList<AvailableTime>();
			list.addAll(timeList);
			Collections.sort(list);
			for (AvailableTime a : list) {
				leftAppointTime.append(a.toString()).append(",");
			}
		} else {
			leftAppointTime.append("[]");
		}
		response.setMsg(response.getMsg()+" "+leftAppointTime.toString());
		return response;
	}
	
}