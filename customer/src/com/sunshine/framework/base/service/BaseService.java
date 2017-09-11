/**
 * <html>
 * <body>
 *  <P> Copyright 2014 广东天泽阳光康众医疗投资管理有限公司. 粤ICP备09007530号-15</p>
 *  <p> All rights reserved.</p>
 *  <p> Created on 2016年4月2日</p>
 *  <p> Created by 申姜</p>
 *  </body>
 * </html>
 */
package com.sunshine.framework.base.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.sunshine.framework.base.entity.BaseEntity;

/**
 * service基类接口
 * @Package: com.sunshine.framework.mvc.service
 * @ClassName: BaseService
 * @Statement: <p>
 *             </p>
 * @JDK version used:
 * @Author: 申姜
 * @Create Date: 2016-4-2
 * @modify By:
 * @modify Date:
 * @Why&What is modify:
 * @Version: 1.0
 */
public interface BaseService<T extends BaseEntity, PK extends Serializable> {
	/**
	 * 查询
	 * 
	 * @param entity
	 * @return
	 */
	public T find(T entity);

	/**
	 * 通过Id查询
	 * 
	 * @param id
	 * @return
	 */
	public T findById(PK id);

	/**
	 * 根据ID集合来查询
	 * 
	 * @param ids
	 * @return
	 */
	public List<T> findByIds(List<PK> ids);

	/**
	 * 查询列表
	 * 
	 * @param entity
	 * @return
	 */
	public List<T> findList(T entity);

	/**
	 * 查询列表
	 * @param params
	 * @return
	 */
	public List<T> findListByParams(Map<String, Object> params);

	/**
	 * 查询列表
	 * 
	 * @param entity
	 * @param pageInfo
	 * @return
	 */
	public PageInfo<T> findListByPage(Map<String, Object> parms, Page<T> page);

	/**
	 * 查询所有记录
	 * 
	 * @return
	 */
	public List<T> findAll();

	/**
	 * 查询总记录数
	 * 
	 * @return
	 */
	public Long count();

	/**
	 * 查询总记录数
	 * 
	 * @param entity
	 * @return
	 */
	public Long count(T entity);

	/**
	 * 添加
	 * 
	 * @param entity
	 */
	public PK insert(T entity);

	/**
	 * 删除
	 * 
	 * @param entity
	 */
	public void delete(T entity);

	/**
	 * 根据Id删除
	 * 
	 * @param id
	 */
	public void deleteById(PK id);

	/**
	 * 根据ID集合删除
	 * 
	 * @param ids
	 */
	public void deleteByIds(List<PK> ids);

	/**
	 * 删除所有记录
	 */
	public void deleteAll();

	/**
	 * 更新
	 * 
	 * @param entity
	 */
	public void update(T entity);

	/**
	 * 更新
	 * @param params
	 */
	public void updateByParams(Map<String, Object> params);

	/**
	 * 检查数据是否已经存在
	 * @param params
	 * @return
	 */
	public boolean check(Map<String, Serializable> params);

	/**
	 * 根据ID集合批量删除
	 * 
	 * @param ids
	 */
	public void batchDelete(List<PK> ids);
	
	/**
	 * 根据ID集合批量逻辑删除<br/>
	 * 执行了update xxx set IS_DELETE = true操作
	 * 
	 * @param map, map.list为List类型，包含ids
	 */
	public void batchLogicalDelete(Map<String, Object> map);

	/**
	 * 批量插入
	 * 
	 * @param entitys
	 */
	public void batchInsert(List<T> entitys);

	/**
	 * 按分表批量插入
	 * @param paramsMap
	 */
	public void batchInsertByMap(Map<String, Object> paramsMap);

	/**
	 * 批量更新
	 * 
	 * @param entitys
	 */
	public void batchUpdate(List<T> entitys);
}
