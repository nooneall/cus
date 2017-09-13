/**
 * <html>
 * <body>
 *  <P> Copyright 2014 广东天泽阳光康众医疗投资管理有限公司. 粤ICP备09007530号-15</p>
 *  <p> All rights reserved.</p>
 *  <p> Created on 2016年11月8日</p>
 *  <p> Created by 申姜</p>
 *  </body>
 * </html>
 */
package com.sunshine.common.msgmanager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sunshine.common.msgmanager.dao.MsgCenterContentDao;
import com.sunshine.common.msgmanager.entity.MsgCenterContent;
import com.sunshine.common.msgmanager.service.MsgCenterContentService;
import com.sunshine.framework.mvc.mysql.dao.BaseDao;
import com.sunshine.framework.mvc.mysql.service.impl.BaseServiceImpl;

/**
 * @Project: sunshine_health 
 * @Package: com.sunshine.common.service.msgmanager.impl
 * @ClassName: MsgCenterContentServiceImpl
 * @Description: <p></p>
 * @JDK version used: 
 * @Author: 申姜
 * @Create Date: 2016年11月8日
 * @modify By:
 * @modify Date:
 * @Why&What is modify:
 * @Version: 1.0
 */
@Service
public class MsgCenterContentServiceImpl extends BaseServiceImpl<MsgCenterContent, String> implements MsgCenterContentService {
	@Autowired
	private MsgCenterContentDao msgCenterContentDao;

	@Override
	protected BaseDao<MsgCenterContent, String> getDao() {
		return msgCenterContentDao;
	}

}