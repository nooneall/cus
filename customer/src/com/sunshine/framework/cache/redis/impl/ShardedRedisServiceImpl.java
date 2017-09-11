/**
 * <html>
 * <body>
 *  <P> Copyright 2014 广东天泽阳光康众医疗投资管理有限公司. 粤ICP备09007530号-15</p>
 *  <p> All rights reserved.</p>
 *  <p> Created on 2016年4月2日</p>
 *  <p> Created by 无名子</p>
 *  </body>
 * </html>
 */
package com.sunshine.framework.cache.redis.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;

import com.alibaba.fastjson.JSON;
import com.sunshine.framework.cache.redis.RedisConstant;
import com.sunshine.framework.cache.redis.RedisService;
import com.sunshine.framework.exception.SystemException;

/**
 * @Package: com.sunshine.framework.cache.redis.impl
 * @ClassName: ShardedRedisServiceImpl
 * @Statement: <p>
 *             </p>
 * @JDK version used:
 * @Author: 无名子
 * @Create Date: 2016-4-2
 * @modify By:
 * @modify Date:
 * @Why&What is modify:
 * @Version: 1.0
 */
public class ShardedRedisServiceImpl implements RedisService {

	private static Logger logger = LoggerFactory.getLogger(RedisService.class);

	/**
	 * redis切片连接池
	 */
	protected ShardedJedisPool shardedJedisPool;

	public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
		this.shardedJedisPool = shardedJedisPool;
	}

	public ShardedJedis getRedisClient() throws SystemException {
		boolean isContinue = true;
		ShardedJedis jedis = null;
		int count = 3;
		try {
			do {
				try {
					jedis = shardedJedisPool.getResource();
					isContinue = false;
				} catch (Exception e) {
					isContinue = true;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					count++;
				}
				if (count > 3) {
					break;
				}
			} while (isContinue);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("redis server connect exception!", e);
		}
		return jedis;
	}

	public void returnResource(ShardedJedis jedis) {
		shardedJedisPool.returnResource(jedis);
	}

	public void returnResource(ShardedJedis jedis, boolean broken) {
		if (broken) {
			shardedJedisPool.returnBrokenResource(jedis);
		} else {
			shardedJedisPool.returnResource(jedis);
		}
	}

	@Override
	public Boolean set(String key, String value) {
		// TODO Auto-generated method stub
		Boolean result = false;

		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			jedis.set(key, value);
			result = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public Boolean set(String key, Object value) {
		// TODO Auto-generated method stub
		Boolean result = false;

		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		System.out.println("key : " + key);
		boolean broken = false;
		try {
			String valueJson = JSON.toJSONString(value);
			jedis.set(key, valueJson);
			result = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * 得到List对象数据
	 * 
	 * @param key
	 * @param clazz
	 *            list中元素的Class
	 * @return
	 */
	public <T> List<T> getList(String key, Class<T> clazz) {
		List<T> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			String value = jedis.get(key);
			result = JSON.parseArray(value, clazz);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * @param key
	 * @param clazz
	 * @return
	 */
	@Override
	public <T> T get(String key, Class<T> clazz) {
		// TODO Auto-generated method stub
		T result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			String value = jedis.get(key);
			result = JSON.parseObject(value, clazz);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public String get(String key) {
		// TODO Auto-generated method stub
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}

		boolean broken = false;
		try {
			result = jedis.get(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/** 
	 * 删除
	 * 
	 * @param keys
	 * @return
	 */
	public Boolean del(String... keys) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return false;
		}
		boolean broken = false;
		try {
			ShardedJedisPipeline pipeLine = jedis.pipelined();
			for (String key : keys) {
				pipeLine.del(key);
			}
			pipeLine.sync();
			result = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * 删除
	 * 
	 * @param keys
	 * @return
	 */
	public Boolean del(String key) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return false;
		}
		boolean broken = false;
		try {
			jedis.del(key);
			result = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * 判断key键是否存在
	 * 
	 * @param key
	 * @return
	 */
	public Boolean exists(String key) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.exists(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String type(String key) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.type(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * 在某段时间后实现
	 * 
	 * @param key
	 * @param unixTime
	 * @return
	 */
	public Long expire(String key, int seconds) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.expire(key, seconds);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * 在某个时间点失效
	 * 
	 * @param key
	 * @param unixTime
	 * @return
	 */
	public Long expireAt(String key, long unixTime) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.expireAt(key, unixTime);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long ttl(String key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.ttl(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public boolean setbit(String key, long offset, boolean value) {
		ShardedJedis jedis = getRedisClient();
		boolean result = false;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.setbit(key, offset, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public boolean getbit(String key, long offset) {
		ShardedJedis jedis = getRedisClient();
		boolean result = false;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;

		try {
			result = jedis.getbit(key, offset);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long setrange(String key, long offset, String value) {
		ShardedJedis jedis = getRedisClient();
		long result = 0;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.setrange(key, offset, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String getrange(String key, long startOffset, long endOffset) {
		ShardedJedis jedis = getRedisClient();
		String result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.getrange(key, startOffset, endOffset);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String getSet(String key, String value) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.getSet(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long setnx(String key, String value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.setnx(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String setex(String key, int seconds, String value) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.setex(key, seconds, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long decrBy(String key, long integer) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.decrBy(key, integer);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long decr(String key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.decr(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long incrBy(String key, long integer) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.incrBy(key, integer);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long incr(String key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.incr(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long append(String key, String value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.append(key, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String substr(String key, int start, int end) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.substr(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hset(String key, String field, String value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hset(key, field, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String hget(String key, String field) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hget(key, field);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hsetnx(String key, String field, String value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hsetnx(key, field, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String hmset(String key, Map<String, String> hash) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hmset(key, hash);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<String> hmget(String key, String... fields) {
		List<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hmget(key, fields);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hincrBy(String key, String field, long value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hincrBy(key, field, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Boolean hexists(String key, String field) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hexists(key, field);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hdel(String key, String... field) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hdel(key, field);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hlen(String key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hlen(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> hkeys(String key) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hkeys(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<String> hvals(String key) {
		List<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hvals(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Map<String, String> hgetAll(String key) {
		Map<String, String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hgetAll(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	// ================list ====== l表示 list或 left, r表示right====================
	public Long rpush(String key, String... vals) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.rpush(key, vals);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long lpush(String key, String string) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lpush(key, string);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long llen(String key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.llen(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<String> lrange(String key, long start, long end) {
		List<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lrange(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String ltrim(String key, long start, long end) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.ltrim(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String lindex(String key, long index) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lindex(key, index);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String lset(String key, long index, String value) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lset(key, index, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long lrem(String key, long count, String value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lrem(key, count, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String lpop(String key) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lpop(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String rpop(String key) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.rpop(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	// return 1 add a not exist value ,
	// return 0 add a exist value
	public Long sadd(String key, String member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.sadd(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> smembers(String key) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.smembers(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long srem(String key, String member) {
		ShardedJedis jedis = getRedisClient();

		Long result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.srem(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String spop(String key) {
		ShardedJedis jedis = getRedisClient();
		String result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.spop(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long scard(String key) {
		ShardedJedis jedis = getRedisClient();
		Long result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.scard(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Boolean sismember(String key, String member) {
		ShardedJedis jedis = getRedisClient();
		Boolean result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.sismember(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String srandmember(String key) {
		ShardedJedis jedis = getRedisClient();
		String result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.srandmember(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zadd(String key, double score, String member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zadd(key, score, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> zrange(String key, int start, int end) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrange(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zrem(String key, String member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrem(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Double zincrby(String key, double score, String member) {
		Double result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zincrby(key, score, member);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zrank(String key, String member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrank(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zrevrank(String key, String member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrank(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> zrevrange(String key, int start, int end) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrange(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrangeWithScores(String key, int start, int end) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zrangeWithScores(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrevrangeWithScores(String key, int start, int end) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zrevrangeWithScores(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zcard(String key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zcard(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Double zscore(String key, String member) {
		Double result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zscore(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<String> sort(String key) {
		List<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.sort(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<String> sort(String key, SortingParams sortingParameters) {
		List<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.sort(key, sortingParameters);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zcount(String key, double min, double max) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zcount(key, min, max);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> zrangeByScore(String key, double min, double max) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScore(key, min, max);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> zrevrangeByScore(String key, double max, double min) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScore(key, max, min);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScore(key, min, max, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
		Set<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScore(key, max, min, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScoreWithScores(key, min, max);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScoreWithScores(key, max, min);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScoreWithScores(key, min, max, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zremrangeByRank(String key, int start, int end) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zremrangeByRank(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zremrangeByScore(String key, double start, double end) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zremrangeByScore(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.linsert(key, where, pivot, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String set(byte[] key, byte[] value) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.set(key, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] get(byte[] key) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.get(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Boolean exists(byte[] key) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.exists(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String type(byte[] key) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.type(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long expire(byte[] key, int seconds) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.expire(key, seconds);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long expireAt(byte[] key, long unixTime) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.expireAt(key, unixTime);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long ttl(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.ttl(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] getSet(byte[] key, byte[] value) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.getSet(key, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long setnx(byte[] key, byte[] value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.setnx(key, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String setex(byte[] key, int seconds, byte[] value) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.setex(key, seconds, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long decrBy(byte[] key, long integer) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.decrBy(key, integer);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long decr(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.decr(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long incrBy(byte[] key, long integer) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.incrBy(key, integer);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long incr(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.incr(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long append(byte[] key, byte[] value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.append(key, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] substr(byte[] key, int start, int end) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.substr(key, start, end);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hset(byte[] key, byte[] field, byte[] value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.hset(key, field, value);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] hget(byte[] key, byte[] field) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.hget(key, field);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hsetnx(byte[] key, byte[] field, byte[] value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.hsetnx(key, field, value);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String hmset(byte[] key, Map<byte[], byte[]> hash) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.hmset(key, hash);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<byte[]> hmget(byte[] key, byte[]... fields) {
		List<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hmget(key, fields);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hincrBy(byte[] key, byte[] field, long value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hincrBy(key, field, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Boolean hexists(byte[] key, byte[] field) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hexists(key, field);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hdel(byte[] key, byte[] field) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hdel(key, field);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long hlen(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hlen(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> hkeys(byte[] key) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hkeys(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Collection<byte[]> hvals(byte[] key) {
		Collection<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.hvals(key);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Map<byte[], byte[]> hgetAll(byte[] key) {
		Map<byte[], byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.hgetAll(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long rpush(byte[] key, byte[] string) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.rpush(key, string);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long lpush(byte[] key, byte[] string) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lpush(key, string);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long llen(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.llen(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<byte[]> lrange(byte[] key, int start, int end) {
		List<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lrange(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String ltrim(byte[] key, int start, int end) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.ltrim(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] lindex(byte[] key, int index) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lindex(key, index);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public String lset(byte[] key, int index, byte[] value) {
		String result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lset(key, index, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long lrem(byte[] key, int count, byte[] value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.lrem(key, count, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] lpop(byte[] key) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.lpop(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] rpop(byte[] key) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.rpop(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long sadd(byte[] key, byte[] member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.sadd(key, member);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> smembers(byte[] key) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.smembers(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long srem(byte[] key, byte[] member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.srem(key, member);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] spop(byte[] key) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.spop(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long scard(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.scard(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Boolean sismember(byte[] key, byte[] member) {
		Boolean result = false;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.sismember(key, member);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public byte[] srandmember(byte[] key) {
		byte[] result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.srandmember(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zadd(byte[] key, double score, byte[] member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zadd(key, score, member);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> zrange(byte[] key, int start, int end) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrange(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zrem(byte[] key, byte[] member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrem(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Double zincrby(byte[] key, double score, byte[] member) {
		Double result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zincrby(key, score, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zrank(byte[] key, byte[] member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrank(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zrevrank(byte[] key, byte[] member) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrank(key, member);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> zrevrange(byte[] key, int start, int end) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrange(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrangeWithScores(byte[] key, int start, int end) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeWithScores(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeWithScores(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zcard(byte[] key) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zcard(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Double zscore(byte[] key, byte[] member) {
		Double result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zscore(key, member);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<byte[]> sort(byte[] key) {
		List<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.sort(key);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
		List<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.sort(key, sortingParameters);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zcount(byte[] key, double min, double max) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {

			result = jedis.zcount(key, min, max);

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScore(key, min, max);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScore(key, min, max, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScoreWithScores(key, min, max);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrangeByScoreWithScores(key, min, max, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScore(key, max, min);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
		Set<byte[]> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScore(key, max, min, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScoreWithScores(key, max, min);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
		Set<Tuple> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zremrangeByRank(byte[] key, int start, int end) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zremrangeByRank(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long zremrangeByScore(byte[] key, double start, double end) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zremrangeByScore(key, start, end);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public Long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value) {
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.linsert(key, where, pivot, value);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public Long persist(String key) {
		// TODO Auto-generated method stub
		Long result = 0l;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.persist(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	/**
	 * 删除keys对应的记录,可以是多个key
	 * 
	 * @param String
	 *            ... keys
	 * @return 删除的记录数
	 * */
	public Long del(byte[]... keys) {
		Long result = 0l;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			ShardedJedisPipeline pipeline = jedis.pipelined();
			for (byte[] key : keys) {
				pipeline.del(key);
			}
			pipeline.sync();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public Set<String> keys(String pattern) {
		// TODO Auto-generated method stub
		Set<String> result = null;
		return result;
	}

	@Override
	public Set<String> sinter(String... keys) {
		// TODO Auto-generated method stub
		Set<String> result = null;
		return result;
	}

	@Override
	public Long sinterstore(String newkey, String... keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long smove(String srckey, String dstkey, String member) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> mget(String... keys) {
		// TODO Auto-generated method stub
		List<String> result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			ShardedJedisPipeline pipeline = jedis.pipelined();
			List<Response<String>> responseList = new ArrayList<Response<String>>(keys.length);
			for (String key : keys) {
				Response<String> res = pipeline.get(key);
				responseList.add(res);
			}
			pipeline.sync();
			if (responseList.size() > 0) {
				result = new ArrayList<String>();
				for (Response<String> response : responseList) {
					result.add(response.get());
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public String mset(String... keysvalues) {
		// TODO Auto-generated method stub
		List<Object> results = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return null;
		}
		boolean broken = false;
		ShardedJedisPipeline pipeline = jedis.pipelined();
		try {
			for (int i = 0; i < keysvalues.length;) {
				pipeline.set(keysvalues[i], keysvalues[i + 1]);
				i = i + 2;
			}
			results = pipeline.syncAndReturnAll();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return JSON.toJSONString(results);
	}

	@Override
	public Long strlen(String key) {
		// TODO Auto-generated method stub
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.strlen(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public Set<String> sunion(String... keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long sunionstore(String newkey, String... keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long zrem(String key) {
		// TODO Auto-generated method stub
		Long result = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			result = jedis.zrem(key);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	@Override
	public <T> T hget(String key, String field, Class<T> clazz) {
		// TODO Auto-generated method stub
		T t = null;
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return null;
		}
		boolean broken = false;
		try {
			String json = jedis.hget(key, field);
			if (StringUtils.isNotBlank(json) && !RedisConstant.CACHE_KEY_NOT_EXIST.equalsIgnoreCase(json)) {
				t = JSON.parseObject(json, clazz);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return t;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sunshine.framework.cache.redis.RedisService#pipelineGetHDatas(java.util
	 * .List, java.lang.String)
	 */
	@Override
	public <T> List<T> pipelineGetHDatas(Collection<String> keys, String field, Class<T> valueType) {
		// TODO Auto-generated method stub
		List<T> dataList = new ArrayList<T>();
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return null;
		}
		boolean broken = false;
		try {
			//分布式直连异步调用
			ShardedJedisPipeline pipeline = jedis.pipelined();
			for (String key : keys) {
				Response<String> response = pipeline.hget(key, field);
				String valString = response.get();
				if (StringUtils.isNotBlank(valString) && !RedisConstant.CACHE_KEY_NOT_EXIST.equalsIgnoreCase(valString)) {
					dataList.addAll(JSON.parseArray(valString, valueType));
				}
			}
			//获取所有的response  
			pipeline.sync();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;
		} finally {
			returnResource(jedis, broken);
		}
		return dataList;
	}

	public Boolean pipelineLDatas(Map<String, List<String>> datas) {
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return false;
		}
		boolean broken = false;
		try {
			ShardedJedisPipeline pipeline = jedis.pipelined();
			for (String dataKey : datas.keySet()) {
				List<String> data = datas.get(dataKey);
				String[] array = new String[data.size()];
				pipeline.rpush(dataKey, data.toArray(array));
			}
			pipeline.sync();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return true;
	}

	/**
	 * 批量把数据写入redis缓存服务器
	 * 
	 * @param datas
	 * @return
	 */
	public Boolean pipelineDatas(Map<String, Map<String, String>> datas) {
		ShardedJedis jedis = getRedisClient();
		if (jedis == null) {
			return false;
		}
		boolean broken = false;
		try {
			ShardedJedisPipeline pipeline = jedis.pipelined();

			for (String dataKey : datas.keySet()) {
				pipeline.hmset(dataKey, datas.get(dataKey));
			}
			pipeline.sync();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return true;
	}

	@Override
	public <T> List<T> pipelinedHash(String cacheKey, Map<String, List<T>> dataMap) {
		// TODO Auto-generated method stub
		ShardedJedis jedis = getRedisClient();
		List<T> result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			String hashValue = null;
			ShardedJedisPipeline pipeline = jedis.pipelined();
			for (String key : dataMap.keySet()) {
				hashValue = JSON.toJSONString(dataMap.get(key));
				pipeline.hset(cacheKey, key, hashValue);
			}
			pipeline.sync();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public List<Object> pipelined(Map<String, String> setDataMap, List<String> getDataKeys) {
		ShardedJedis jedis = getRedisClient();
		List<Object> result = null;
		if (jedis == null) {
			return result;
		}
		boolean broken = false;
		try {
			ShardedJedisPipeline pipeline = jedis.pipelined();
			for (String key : setDataMap.keySet()) {
				pipeline.set(key, setDataMap.get(key));
			}
			List<Response<String>> reses = new ArrayList<Response<String>>();
			if (getDataKeys != null && getDataKeys.size() > 0) {
				result = new ArrayList<Object>();
				for (String key : getDataKeys) {
					reses.add(pipeline.get(key));
				}
			}
			pipeline.sync();
			for (Response<String> res : reses) {
				result.add(res.get());
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			broken = true;

		} finally {
			returnResource(jedis, broken);
		}
		return result;
	}

	public ShardedJedisPool getShardedJedisPool() {
		return shardedJedisPool;
	}

	public JedisPool getRedisPool() throws SystemException {
		// TODO Auto-generated method stub
		return null;
	}

	public ShardedJedisPool getShardedRedisPool() throws SystemException {
		// TODO Auto-generated method stub
		return shardedJedisPool;
	}

}
