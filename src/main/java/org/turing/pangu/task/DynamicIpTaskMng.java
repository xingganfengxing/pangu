package org.turing.pangu.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.turing.pangu.bean.TaskConfigureBean;
import org.turing.pangu.controller.common.PhoneTask;
import org.turing.pangu.controller.common.VpnTask;
import org.turing.pangu.controller.pc.request.VpnLoginReq;
import org.turing.pangu.controller.pc.response.VpnOperUpdateRsp;
import org.turing.pangu.controller.phone.request.TaskFinishReq;
import org.turing.pangu.engine.IpMngEngine;
import org.turing.pangu.engine.TaskEngine;
import org.turing.pangu.engine.TaskListSort;
import org.turing.pangu.engine.TimeZoneMng;
import org.turing.pangu.model.App;
import org.turing.pangu.model.Device;
import org.turing.pangu.model.Platform;
import org.turing.pangu.model.RemainVpn;
import org.turing.pangu.model.Task;
import org.turing.pangu.service.AppService;
import org.turing.pangu.service.DeviceService;
import org.turing.pangu.service.PlatformService;
import org.turing.pangu.service.RemainVpnService;
import org.turing.pangu.service.TaskService;
import org.turing.pangu.task.TaskIF;
import org.turing.pangu.utils.DateUtils;
import org.turing.pangu.utils.RandomUtils;
/*
 * 任务引擎，负责每日任务生成,配置任务,跟踪任务进展,
 * */
public class DynamicIpTaskMng implements TaskIF{
	private static final Logger logger = Logger.getLogger(DynamicIpTaskMng.class);
	private static DynamicIpTaskMng mInstance = new DynamicIpTaskMng();
	private List<VpnTask> vpnTaskList = new ArrayList<VpnTask>();
	private DateUpdateListen mListen = null;
	
	public static DynamicIpTaskMng getInstance(){
		if(null == mInstance)
			mInstance = new DynamicIpTaskMng();
		return mInstance;
	}
	public void setDataUpdateListen(DateUpdateListen listen){
		mListen = listen;
	}
	public synchronized String addVpnTask(VpnLoginReq req,String remoteIp,String realIp){
		logger.info("addVpnTask---000");
		// 此IP不能运行任务了
		if(false == IpMngEngine.getInstance().isCanGetTask(remoteIp)){
			return null;
		}
		VpnTask task = new VpnTask();
		task.setDeviceId(req.getDeviceId());
		task.setOperType(req.getOperType());
		task.setRemoteIp(remoteIp);
		task.setRealIp(realIp);
		task.setToken(RandomUtils.getRandom(TaskEngine.VPN_TOKEN_LENGH));
		task.setCreateTime(new Date());
		logger.info("addVpnTask---001--"+task.toString());
		for (int i = vpnTaskList.size()-1; i >=0; i--){
			VpnTask tmp = vpnTaskList.get(i);
			if(tmp.getDeviceId().equals(task.getDeviceId())){
				vpnTaskList.remove(tmp); // 删除原来的VPN task
				logger.info("addVpnTask---002--del old task");
			}
		}
		vpnTaskList.add(task);
		logger.info("addVpnTask---end");
		return task.getToken();
	}

	public synchronized VpnOperUpdateRsp vpnIsNeedSwitch(String token){
		logger.info("vpnIsNeedSwitch---000--"+token);
		VpnOperUpdateRsp dataRsp = new VpnOperUpdateRsp();
		dataRsp.setIsSwitchVpn(1);
		for(VpnTask task :vpnTaskList){
			if(task.getToken().equals(token)){
				if(task.getPhoneTaskList() == null || task.getPhoneTaskList().size() == 0){
					logger.info("vpnIsNeedSwitch---false---001---end");
					dataRsp.setIsSwitchVpn(0);
					dataRsp.setFinishedTaskCount(0);
					dataRsp.setTaskTotal(0);
				}
				dataRsp.setTaskTotal(task.getPhoneTaskList().size());
				for(PhoneTask pTask:task.getPhoneTaskList()){
					dataRsp.setFinishedTaskCount(dataRsp.getFinishedTaskCount()+1);
					if(pTask.getIsFinished() == 0 && false == isTimeOut(task)){ // 发现模拟器还有未完成的任务
						logger.info("vpnIsNeedSwitch---false---002---end");
						dataRsp.setIsSwitchVpn(0);
						dataRsp.setFinishedTaskCount(dataRsp.getFinishedTaskCount()-1);
					}
				}
			}
		}
		logger.info("vpnIsNeedSwitch---true---end");
		return dataRsp;
	}
	public synchronized boolean switchVpnFinish(String token,String remoteIp,String realIp){
		// 此IP不能运行任务了
		if(false == IpMngEngine.getInstance().isCanGetTask(remoteIp)){
			return false;
		}
		logger.info("switchVpnFinish---000");
		for(VpnTask task :vpnTaskList){
			if(task.getToken().equals(token)){
				if(remoteIp.equals(task.getRemoteIp())){
					logger.info("switchVpnFinish|newRemoteIp == oldRemoteIp");
				}
				if(realIp.equals(task.getRealIp())){
					logger.info("switchVpnFinish|newRealIp == oldRealIp");
				}
				logger.info("switchVpnFinish|remoteIp:"+remoteIp+"|realIp:"+realIp);
				task.setRemoteIp(remoteIp);
				task.getPhoneTaskList().clear();//清空所有任务
				task.setRealIp(realIp);
				task.setCreateTime(new Date());//重新设置超时起点时间
				break;
			}
		}
		logger.info("switchVpnFinish---end");
		return true;
	}

	// 手机端一个任务完成
	public synchronized void taskFinish(TaskFinishReq req,String remoteIp,String realIp){
		logger.info("taskFinish---000");
		logger.info("taskFinish---taskId:"+req.getTaskId()+"--remoteIp:"+remoteIp+"--realIp:"+realIp);
		for(VpnTask task:vpnTaskList){
			if(task.getToken().equals(req.getVpnToken())){
				for(PhoneTask tmpTask:task.getPhoneTaskList()){
					if(tmpTask.getTaskId().equals(req.getTaskId())){
						logger.info("taskFinish---001--findandsettastkFinished");
						if(req.getIsFinished() == 1 ){
							tmpTask.setIsFinished(1);//找到任务并设为完成状态
							mListen.updateExecuteTask(tmpTask); // 更新执行任务
						}
					}
				}
			}
		}
		logger.info("taskFinish---end");
	}
	//取一个任务
	public synchronized PhoneTask getTask(String deviceId,String remoteIp,String realIp){
		// 1. 先查VPN用途
		PhoneTask pTask = null;
		logger.info("getTask---000");
		logger.info("getTask---001--deviceId:"+deviceId+"--remoteIp:"+remoteIp+"--realIp:"+realIp);
		for(VpnTask task:vpnTaskList){
			if(task.getRemoteIp().equals(remoteIp)){
				if(task.getPhoneTaskList().size() > TaskEngine.getInstance().appList.size() * TimeZoneMng.getInstance().getTimeZoneWeight() ){
					return pTask;
				}
				for(PhoneTask tmpTask:task.getPhoneTaskList()){
					if(tmpTask.getDeviceId().equals(deviceId)){ // 发现取过任务了
						if(tmpTask.getIsFinished() == 0){ // 任务没完成,返回原来的任务
							logger.info("getTask---end - repeat task not finished,return old task");
							return tmpTask; 
						}
						else{
							logger.info("getTask---end - repeat task finished,return null task");
							return null;// 任务完成,这种情况下不给任务
						}
					}
				}
				pTask = getTask(task,deviceId,task.getOperType(),remoteIp,realIp);
				if(pTask != null){
					logger.info("getTask---002--"+pTask.toString());
					task.getPhoneTaskList().add(pTask);
				}else{
					logger.info("getTask---not task");
				}
			}
		}
		
		logger.info("getTask---end");
		return pTask;
	}
	
	private synchronized boolean isTimeOut(VpnTask task){
		Date nowTime = new Date();
		logger.info("isTimeOut---000--OperType:"+task.getOperType());
		switch(task.getOperType()){
		case TaskEngine.INCREMENT_MONEY_TYPE:
			return (nowTime.getTime() - task.getCreateTime().getTime() > TimeZoneMng.INCREMENT_MONEY_TIMEOUT)?true:false;
		case TaskEngine.INCREMENT_WATERAMY_TYPE:
			return (nowTime.getTime() - task.getCreateTime().getTime() > TimeZoneMng.INCREMENT_WATERAMY_TIMEOUT)?true:false;	
		case TaskEngine.STOCK_MONEY_TYPE:	
			return (nowTime.getTime() - task.getCreateTime().getTime() > TimeZoneMng.STOCK_MONEY_TIMEOUT)?true:false;
		case TaskEngine.STOCK_WATERAMY_TYPE:
			return (nowTime.getTime() - task.getCreateTime().getTime() > TimeZoneMng.STOCK_WATERAMY_TIMEOUT)?true:false;
		}
		return true;
	}
	private synchronized PhoneTask getTask(VpnTask task,String deviceId,int operType,String remoteIp,String realIp){
		PhoneTask pTask = new PhoneTask();
		long appId = getOptimalAppId(task,remoteIp,realIp);
		if(appId == 0L){
			return null;
		}
		pTask.setDeviceId(deviceId);
		pTask.setVpnToken(task.getToken());
		pTask.setOperType(operType);
		pTask.setTaskId(task.getToken() + RandomUtils.getRandom(TaskEngine.PHONE_TASKID_LENGH/2));//32位
		pTask.setApp(TaskEngine.getInstance().getAppInfo(appId));
		pTask.setOperType(task.getOperType());
		pTask.setTimes(1);// 暂定一次
		pTask.setSpanTime(5);//暂定5S 
		pTask.setStockInfo(null);
		return pTask;
	}

	// 获得最优 appID
	private synchronized long getOptimalAppId(VpnTask task,String remoteIp,String realIp){
		// -- 排序
		logger.info("getOptimalAppId---000");
		logger.info("getOptimalAppId---001--operType:"+task.getOperType()+"remoteIp:"+remoteIp+"realIp:"+realIp);
		TaskListSort.taskSort(TaskEngine.getInstance().todayTaskList, task.getOperType());//对任务列表排序
		{// 处理增量
			logger.info("getOptimalAppId---004--INCREMENT");
			int flag = 0;
			for(Task dbTask:TaskEngine.getInstance().todayTaskList){
				flag = 0;
				for(PhoneTask tmpTask:task.getPhoneTaskList()){
					if(tmpTask.getApp().getId() == dbTask.getAppId()){
						flag = 1;
						logger.info("getOptimalAppId---005-- INCREMENT existed");
						break;
					}
				}
				if(flag == 0 && isHavaTaskByOperType(task.getOperType(),dbTask)){
					mListen.updateAllocTask(task.getOperType(),dbTask); // 对应派发 ++ 
					logger.info("getOptimalAppId---end--return INCREMENT ");
					return dbTask.getAppId();
				}
			}
		}
		logger.info("getOptimalAppId---end--not task send ");
		return 0L;
	}

	// 是否还有该类型的任务
	private synchronized boolean isHavaTaskByOperType(int type,Task dbTask){
		switch(type){
		case TaskEngine.INCREMENT_MONEY_TYPE:
			return (dbTask.getIncrementMoney() - dbTask.getAllotIncrementMoney()) > 0 ? true:false;
		case TaskEngine.INCREMENT_WATERAMY_TYPE:
			return (dbTask.getIncrementWaterAmy() - dbTask.getAllotIncrementWaterAmy()) > 0 ? true:false;
		case TaskEngine.STOCK_MONEY_TYPE:	
			return (dbTask.getStockMoney() - dbTask.getAllotStockMoney()) > 0 ? true:false;
		case TaskEngine.STOCK_WATERAMY_TYPE:
			return (dbTask.getStockWaterAmy() - dbTask.getAllotStockWaterAmy()) > 0 ? true:false;
		}
		return false;
	}
}