/**
 * 
 * Title：User
 * Copyright: Copyright (c) 2014
 * Company: 深电中心
 * @author axi
 * @version 1.0, 2016年08月23日 
 * @since 2016年08月23日 
 */

package org.turing.pangu.dao;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.turing.pangu.mapper.ComputerMapper;
import org.turing.pangu.model.Computer;

@Repository
public class ComputerDaoImpl extends BaseDaoImpl<Computer, Long> implements ComputerDao {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(ComputerDaoImpl.class);

	@Autowired
	private ComputerMapper mapper;
	
	@Autowired
	public void setComputerMapper(ComputerMapper mapper) {
		super.setBaseMapper(mapper);
	}

}
