package com.datahub.infra.coredb.dao.impl;

import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.core.model.CredentialInfo;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CredentialDaoImpl implements CredentialDao {

	private SqlSessionTemplate sqlSessionTemplate;

	@Autowired
	public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Override
	public List<CredentialInfo> getCredentials(Map<String, Object> params) {
		return this.sqlSessionTemplate.selectList("getCredentials", params);
	}

	@Override
	public int getTotal(Map<String, Object> params) {
		return sqlSessionTemplate.selectOne("getCredentialTotal", params);
	}

	@Override
	public CredentialInfo getCredentialInfo(Map<String, Object> params) {
		return this.sqlSessionTemplate.selectOne("getCredentials", params);
	}

	@Override
	public int deleteCredential(CredentialInfo info) {
		return this.sqlSessionTemplate.delete("deleteCredential", info);
	}
}
