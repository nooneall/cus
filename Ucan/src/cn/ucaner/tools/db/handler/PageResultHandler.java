/**
 * <html>
 * <body>
 *  <P> Copyright 1994 JsonInternational</p>
 *  <p> All rights reserved.</p>
 *  <p> Created on 19941115</p>
 *  <p> Created by Jason</p>
 *  </body>
 * </html>
 */
package cn.ucaner.tools.db.handler;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.ucaner.tools.db.Entity;
import cn.ucaner.tools.db.PageResult;

/**
 * 分页结果集处理类 ，处理出的结果为PageResult
 * @author loolly
 *
 */
public class PageResultHandler implements RsHandler<PageResult<Entity>>{
	
	PageResult<Entity> pageResult;
	
	/**
	 * 创建一个 EntityHandler对象<br>
	 * 结果集根据给定的分页对象查询数据库，填充结果
	 * @param pageResult 分页结果集空对象
	 * @return EntityHandler对象
	 */
	public static PageResultHandler create(PageResult<Entity> pageResult) {
		return new PageResultHandler(pageResult);
	}
	
	/**
	 * 构造<br>
	 *  结果集根据给定的分页对象查询数据库，填充结果
	 * @param pageResult 分页结果集空对象
	 */
	public PageResultHandler(PageResult<Entity> pageResult) {
		this.pageResult = pageResult;
	}

	@Override
	public PageResult<Entity> handle(ResultSet rs) throws SQLException {
		return HandleHelper.handleRs(rs, pageResult);
	}
}