package com.lhs.pay.common.core.dao;


import com.alibaba.druid.pool.DruidDataSource;
import com.lhs.pay.common.core.mybatis.interceptor.ExecutorInterceptor;
import com.lhs.pay.common.entity.BaseEntity;
import com.lhs.pay.common.exceptions.BizException;
import com.lhs.pay.common.page.PageBean;
import com.lhs.pay.common.page.PageParam;
import org.apache.ibatis.jdbc.SqlRunner;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BaseDaoImpl
 *
 * 数据访问层基础支撑接口实现类
 *
 * @author longhuashen
 * @since 16/3/21
 */
public abstract class BaseDaoImpl<T extends BaseEntity> extends SqlSessionDaoSupport implements BaseDao<T> {

    public static final String SQL_INSERT = "insert";

    public static final String SQL_BATCH_INSERT = "batchInsert";

    public static final String SQL_UPDATE = "update";

    public static final String SQL_GET_BY_ID = "getById";

    public static final String SQL_DELETE_BY_ID = "deleteById";

    public static final String SQL_LIST_PAGE = "listPage";

    public static final String SQL_LIST_By = "listBy";

    /**
     * 根据当前分页参数进行统计
     */
    public static final String SQL_COUNT_BY_PAGE_PARAM = "countByPageParam";

    /**
     * 注入sessionTemplate实例(要求Spring中进行sessionTemplate的配置).<br/>
     * 可以调用sessionTemplate完成数据库操作.
     */
    @Autowired
    private SqlSessionTemplate sessionTemplate;

    @Autowired
    protected SqlSessionFactory sqlSessionFactory;

    @Autowired
    private DruidDataSource druidDataSource;


    public SqlSessionTemplate getSessionTemplate() {
        return sessionTemplate;
    }

    public void setSessionTemplate(SqlSessionTemplate sessionTemplate) {
        this.sessionTemplate = sessionTemplate;
    }

    public SqlSession getSqlSession() {
        return super.getSqlSession();
    }


    @Override
    public long insert(T entity) {
        if (entity == null) {
            throw new RuntimeException("T is null");
        }

        int result = sessionTemplate.insert(getStatement(SQL_INSERT), entity);

        if (result <= 0) {
            throw BizException.DB_INSERT_RESULT_0;
        }

        if (entity != null && entity.getId() != null && result > 0) {
            return entity.getId();
        }
        return result;
    }

    public String getStatement(String sqlId) {
        String name = this.getClass().getName();

        StringBuffer sb = new StringBuffer().append(name).append(".").append(sqlId);

        return sb.toString();
    }

    @Override
    public long insert(List<T> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }

        int result = sessionTemplate.insert(getStatement(SQL_BATCH_INSERT), list);

        if (result <= 0) {
             throw BizException.DB_INSERT_RESULT_0;
        }
        return result;
    }

    @Override
    public long update(T entity) {
        if (entity == null) {
            throw new RuntimeException("entity is null");
        }

        int result = sessionTemplate.update(getStatement(SQL_UPDATE), entity);
        if (result <= 0) {
            throw BizException.DB_UPDATE_RESULT_0;
        }
        return result;
    }

    @Override
    public long update(List<T> list) {
        if (list == null || list.size() <= 0) {
            return 0;
        }

        int result = 0;
        for (int i =0; i < list.size(); i++) {
            this.update(list.get(i));
            result++;
        }
        return result;
    }

    @Override
    public T getById(long id) {
        return sessionTemplate.selectOne(getStatement(SQL_GET_BY_ID), id);
    }

    @Override
    public long deleteById(long id) {
        return (long) sessionTemplate.delete(getStatement(SQL_DELETE_BY_ID), id);
    }

    @Override
    public PageBean listPage(PageParam pageParam, Map<String, Object> paramMap) {

        if (paramMap == null) {
            paramMap = new HashMap<>();
        }

        //获取分页数据，切勿换成 sessionTemplate对象
        List<Object> list = getSqlSession().selectList(getStatement(SQL_LIST_PAGE), paramMap,
                new RowBounds((pageParam.getPageNum() -1) * pageParam.getNumPerPage(),pageParam.getNumPerPage()));

        //统计总记录数
        Object countObject = (Object) getSqlSession().selectOne(getStatement(SQL_LIST_PAGE), new ExecutorInterceptor.CountParameter(paramMap));
        Long count = Long.valueOf(countObject.toString());

        //是否统计当前分页条件下的数据 1：是，其他否
        Object isCount = paramMap.get("isCount");
        if (isCount != null && "1".equals(isCount.toString())) {
            Map<String, Object> countResultMap = sessionTemplate.selectOne(getStatement(SQL_COUNT_BY_PAGE_PARAM), paramMap);
            return new PageBean(pageParam.getPageNum(), pageParam.getNumPerPage(), count.intValue(), list, countResultMap);
        } else {
            return new PageBean(pageParam.getPageNum(), pageParam.getNumPerPage(), count.intValue(), list);
        }
    }

    @Override
    public PageBean listPage(PageParam pageParam, Map<String, Object> paramMap, String sqlId) {
        if (pageParam == null) {
            paramMap = new HashMap<>();
        }

        //获取分页数据
        List<Object> list = getSqlSession().selectList(getStatement(sqlId), paramMap
                , new RowBounds((pageParam.getPageNum() - 1) * pageParam.getNumPerPage(), pageParam.getNumPerPage()));

        //统计总记录数
        Object countObject = (Object) getSqlSession().selectOne(getStatement(sqlId), new ExecutorInterceptor.CountParameter(pageParam));
        Long count = Long.valueOf(countObject.toString());

        return new PageBean(pageParam.getPageNum(), pageParam.getNumPerPage(), count.intValue(), list);
    }

    @Override
    public List<T> listBy(Map<String, Object> paramMap) {
        return (List) this.listBy(paramMap, SQL_LIST_By);
    }

    @Override
    public List<Object> listBy(Map<String, Object> paramMap, String sqlId) {
        if (paramMap == null) {
            paramMap = new HashMap<>();
        }

        return sessionTemplate.selectList(getStatement(sqlId), paramMap);
    }

    @Override
    public T getBy(Map<String, Object> paramMap) {
        return (T) this.getBy(paramMap, SQL_LIST_By);
    }

    @Override
    public Object getBy(Map<String, Object> paramMap, String sqlId) {
        if (paramMap == null || paramMap.isEmpty()) {
            return null;
        }
        return this.getSqlSession().selectOne(getStatement(sqlId), paramMap);
    }

    @Override
    public String getSeqNextValue(String seqName) {
        boolean isClosedConn = false;
        //获取当前线程的连接
        Connection connection = this.sessionTemplate.getConnection();
        //获取Mybatis的SQLRunner类
        SqlRunner sqlRunner = null;
        try {
            //要执行的sql
            String sql = "";
            //数据库驱动类
            String driverClass = druidDataSource.getDriver().getClass().getName();
            //不同的数据库，拼接sql语句
            if (driverClass.equals("com.ibm.db2.jcc.DB2Driver")) {
                sql = " VALUES" + seqName.toUpperCase() + ".NEXTVAL";
            }

            if (driverClass.equals("oracle.jdbc.OracleDriver")) {
                sql = "SELECT" + seqName.toUpperCase() + ".NEXTVAL FROM DUAL";
            }

            if (driverClass.equals("com.mybatis.jdbc.Driver")) {
                sql = "SELECT FUN_SEQ('" + seqName.toUpperCase() + "')";
            }

            //如果状态为关闭，则需要从新打开一个连接
            if (connection.isClosed()) {
                connection = sqlSessionFactory.openSession().getConnection();
                isClosedConn = true;
            }

            sqlRunner = new SqlRunner(connection);
            Object[] args = {};
            //执行sql语句
            Map<String, Object> params = sqlRunner.selectOne(sql, args);
            for (Object o : params.values()) {
                return o.toString();
            }
            return null;
        } catch (Exception e) {
            throw BizException.DB_GET_SEQ_NEXT_VALUE_ERROR.newInstance("获取序列出现错误！序列名称：{%s}", seqName);
        } finally {
            if (isClosedConn) {
                sqlRunner.closeConnection();
            }
        }
    }
}
