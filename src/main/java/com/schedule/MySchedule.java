package com.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.alibaba.fastjson.JSONArray;

/**
 * 测试排班系统
 * @author Administrator
 */
public class MySchedule {
	//班次类型
	private static Map<String,String> scheduleTypes = new HashMap<String,String>();
	
	private static final String NEW_ADD = "新增";
	private static final String FORMULA = "配方";
	private static final String DRUG_TRANS = "调药";
	private static final String MOVE = "机动";
	private static final String PRE_FLUID = "配液";
	private static final String FRONT = "前台";
	private static final String NIGHT_REST = "夜休";
	private static final String DAY_REST = "放假";
	
	//工作日类型
	private static final String WORKDAY = "workday";
	private static final String WEEKEND = "weekend";
	
	
	public static void main(String[] args) {
		//最终排班结果
		//key：时间，例如周一
		//value：排班结果，Map对象<String,List<String>>
		//					key:班次名称
		//					value:班次人员
		
		Map<String,Object> finalSchedule = new HashMap<String,Object>();
		//初始化班次以及班次信息
		initSchedules();
		//进行排班
		doSchedule(finalSchedule);
	}

	/**
	 * 进行排班
	 */
	private static void doSchedule(Map<String,Object> finalSchedule) {
		//先进行特殊排班
		doSpecialSchedule(finalSchedule,"S1");
		//再进行放假排班
		doRestScheduleNew(finalSchedule);
		//定义一周7天
		for(int i=1;i<=7;i++){
			doEverySchedule(finalSchedule,i);
		}
		//打印结果
		printAllSchedule(finalSchedule);
		//导出结果
		exportResult(finalSchedule, "D:/test.xlsx");
	}
	
	
	/**
	 * 进行放假排班优化
	 * 规则：
	 * 	每人放假2天，至少1天周日，周四不允许放假
	 * @param finalSchedule
	 */
	private static void doRestScheduleNew(Map<String, Object> finalSchedule) {
		//获取所有的排班人员
		List<String> normalPerson = getNormalWorker();
		List<String> specialPerson = getSpecialWorker();
		List<String> totalWorker = new ArrayList<String>();
		totalWorker.addAll(normalPerson);
		totalWorker.addAll(specialPerson);
		//把S1放头位，优先排
		totalWorker.remove("S1");
		totalWorker.add(0,"S1");
		List<String> workDayList = getWorkDayCanRest();
		List<String> weekendDayList = getWeekendDayCanRest();
		//循环所有排班人员，进行工作日、周日的休假排班，每个人排班一天平日，一天周末
		for(String worker:totalWorker){
			//针对S1进行特殊处理，计算出固定的放假日期
			if("S1".equals(worker)){
				List<String> dayRestList = getCanRestDay(finalSchedule,worker);
				for(String restDay:dayRestList){
					setWorkerToScheduleListByDay(finalSchedule, restDay, DAY_REST, worker);
				}
			}else{
				//进行平日排班
				doRestScheduleNew(workDayList,WORKDAY,finalSchedule,worker);
				//进行周末排班
				doRestScheduleNew(weekendDayList,WEEKEND,finalSchedule,worker);
			}
		}
		
	}
	
	/**
	 * 根据已排班日期，计算出可以放假的日期
	 * @param finalSchedule
	 * @param worker
	 * @return
	 */
	private static List<String> getCanRestDay(Map<String, Object> finalSchedule, String worker) {
		List<String> restDay = new ArrayList<String>();
		for(int i=1;i<=7;i++){
			Map<String,List<String>> tempMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
			if(tempMap==null || tempMap.isEmpty()){
				restDay.add(String.valueOf(i));
				continue;
			}
			boolean hasScheduled = false;
			for(Map.Entry<String,List<String>> map:tempMap.entrySet()){
				List<String> tempList = map.getValue();
				if(tempList!=null && tempList.contains(worker)){
					hasScheduled = true;
					break;
				}
			}
			if(!hasScheduled){
				restDay.add(String.valueOf(i));
			}
		}
		return restDay;
	}

	private static void doRestScheduleNew(List<String> dayList,String type,Map<String, Object> finalSchedule,String worker){
		if(dayList==null || dayList.isEmpty()){
			if(type.equals(WORKDAY)){
				dayList.addAll(getWorkDayCanRest());
			}else if(type.equals(WEEKEND)){
				dayList.addAll(getWeekendDayCanRest());
			}
		}
		//System.out.println(type+":"+worker+":"+JSONArray.toJSONString(dayList));
		//计算此人在哪天放假
		String restDay = getPreRandomFromList(dayList,finalSchedule,worker);
		setWorkerToScheduleListByDay(finalSchedule, restDay, DAY_REST, worker);
	}
	
	/**
	 * 将某人添加到某一天的某一班次上去
	 * @param finalSchedule 班次map
	 * @param day 某天
	 * @param scheduleType 某一班次
	 * @param workerName 某人
	 */
	private static void setWorkerToScheduleListByDay(Map<String, Object> finalSchedule,String day,String scheduleType,String workerName){
		Map<String,List<String>> daySchedule = (Map<String, List<String>>) finalSchedule.get(day);
		if(daySchedule==null || daySchedule.isEmpty()){
			daySchedule = new HashMap<String,List<String>>();
		}
		List<String> dayList = daySchedule.get(scheduleType);
		if(dayList==null || dayList.isEmpty()){
			dayList = new ArrayList<String>();
		}
		//将此人放入班次
		dayList.add(workerName);
		daySchedule.put(scheduleType, dayList);
		finalSchedule.put(day, daySchedule);
	}
	
	/**
	 * 进行放假排班
	 * 规则：
	 * 	每人放假2天，1天平日，1天周末，周四不允许放假
	 * @param finalSchedule
	 */
	private static void doRestSchedule(Map<String, Object> finalSchedule) {
		//获取所有的排班人员
		List<String> normalPerson = getNormalWorker();
		List<String> specialPerson = getSpecialWorker();
		List<String> totalWorker = new ArrayList<String>();
		totalWorker.addAll(normalPerson);
		totalWorker.addAll(specialPerson);
		//把S1放头位，优先排
		totalWorker.remove("S1");
		totalWorker.add(0,"S1");
		List<String> workDayList = getWorkDayCanRest();
		List<String> weekendDayList = getWeekendDayCanRest();
		//循环所有排班人员，进行工作日、周日的休假排班，每个人排班一天平日，一天周末
		for(String worker:totalWorker){
			//进行平日排班
			doRestSchedule(workDayList,WORKDAY,finalSchedule,worker);
			//进行周末排班
			doRestSchedule(weekendDayList,WEEKEND,finalSchedule,worker);
		}
		
	}
	
	private static void doRestSchedule(List<String> dayList,String type,Map<String, Object> finalSchedule,String worker){
		if(dayList==null || dayList.isEmpty()){
			if(type.equals(WORKDAY)){
				dayList.addAll(getWorkDayCanRest());
			}else if(type.equals(WEEKEND)){
				dayList.addAll(getWeekendDayCanRest());
			}
		}
		//System.out.println(type+":"+worker+":"+JSONArray.toJSONString(dayList)+":dayList.isEmpty()="+dayList.isEmpty());
		//计算此人在哪天放假
		String restDay = getPreRandomFromList(dayList,finalSchedule,worker);
		Map<String,List<String>> restDaySchedule = (Map<String, List<String>>) finalSchedule.get(restDay);
		if(restDaySchedule==null || restDaySchedule.isEmpty()){
			restDaySchedule = new HashMap<String,List<String>>();
		}
		List<String> restDayList = restDaySchedule.get(DAY_REST);
		if(restDayList==null || restDayList.isEmpty()){
			restDayList = new ArrayList<String>();
		}
		//将此人放入放假班次
		restDayList.add(worker);
		restDaySchedule.put(DAY_REST, restDayList);
		finalSchedule.put(restDay, restDaySchedule);
	}

	/**
	 * 从数组中获取随机一个排班日期返回并移除
	 * @param workDayList
	 * @return
	 */
	private static String getPreRandomFromList(List<String> list,Map<String, Object> finalSchedule,String worker) {
		String day = list.get((int)(Math.random()*list.size()));
		//如果循环到该日期该人员没有排班，则返回该日期，进行放假排班
		while(true){
			if(!hasScheduledThisDay(finalSchedule,day,worker)){
				break;
			}
			day = list.get((int)(Math.random()*list.size()));
		}
		list.remove(day);
		return day;
	}
	
	private static boolean hasScheduledThisDay(Map<String, Object> finalSchedule,String day,String worker){
		Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(day);
		if(scheduleMap==null || scheduleMap.isEmpty()){
			return false;
		}
		for(Map.Entry<String,List<String>> map:scheduleMap.entrySet()){
			List<String> workerList = map.getValue();
			if(workerList==null || workerList.isEmpty()){
				continue;
			}
			if(workerList.contains(worker)){
				return true;
			}
		}
		return false;
	}

	/**
	 * 进行特殊排班
	 * S1 每周4次配液+1次机动
	 * @param finalSchedule
	 */
	private static void doSpecialSchedule(Map<String, Object> finalSchedule,String worker) {
		//进行随机组合
		List<Map<String,String>> randomWeek = getRandomWeekNew();
		for(Map<String,String> map:randomWeek){
			for(Entry<String, String> childMap:map.entrySet()){
				String scheduleName = childMap.getValue();
				Map<String,List<String>> schedulMap = new HashMap<String,List<String>>();
				List<String> workerList = new ArrayList<String>();
				workerList.add(worker);
				schedulMap.put(scheduleName, workerList);
				finalSchedule.put(childMap.getKey(), schedulMap);
			}
		}
	}

	/**
	 * 生成随机星期和排班类型，周末不能同时占2天
	 * @return
	 */
	private static List<Map<String,String>> getRandomWeek() {
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		List<String> list = new ArrayList<String>();
		Random random = new Random();
		int removeRand = 1+random.nextInt(5);
		list.add("4");//周四全勤
		for(int i=1;i<=5;i++){
			if(list.size()==4){
				break;
			}
			if(i!=removeRand && i!=4){
				list.add(String.valueOf(i));
			}
		}
		list.add(String.valueOf(6+random.nextInt(2)));
		int oneTimeSchedulIndex = 1+random.nextInt(5);
		for(String week:list){
			Map<String,String> map = new HashMap<String,String>();
			if(week.equals(String.valueOf(oneTimeSchedulIndex))){
				map.put(week, MOVE);
			}else{
				map.put(week, PRE_FLUID);
			}
			result.add(map);
		}
		return result;
	}
	
	
	/**
	 * 生成随机星期和排班类型，周末不能同时占2天
	 * 优化：周末0-1天
	 * @return
	 */
	private static List<Map<String,String>> getRandomWeekNew() {
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		Random random = new Random();
		List<String> totalList = new ArrayList<String>();
		//机动班的日期
		int oneTimeSchedulIndex = 1+random.nextInt(4);
		//计算周末是否上班，如果上班，算哪一天上班，只能一天
		List<String> weekendList = new ArrayList<String>();
		if(isWeekendWork()){
			weekendList.add(String.valueOf(6+random.nextInt(2)));
		}
		List<String> normalList = new ArrayList<String>();
		if(weekendList.size()==0){
			for(int i=1;i<=5;i++){
				normalList.add(String.valueOf(i));
			}
		}else{
			int removeRand = 1+random.nextInt(5);
			normalList.add("4");//周四全勤
			for(int i=1;i<=5;i++){
				if(normalList.size()==4){
					break;
				}
				if(i!=removeRand && i!=4){
					normalList.add(String.valueOf(i));
				}
			}
		}
		totalList.addAll(normalList);
		totalList.addAll(weekendList);
		for(int j=0;j<totalList.size();j++){
			String week = totalList.get(j);
			Map<String,String> map = new HashMap<String,String>();
			if(oneTimeSchedulIndex==j){
				map.put(week, MOVE);
			}else{
				map.put(week, PRE_FLUID);
			}
			result.add(map);
		}
		return result;
	}
	
	/**
	 * 随机生成周末是否上班
	 * @return
	 */
	private static boolean isWeekendWork(){
		Random random = new Random();
		int i = random.nextInt(2);
		if(i==0){
			return false;
		}
		return true;
	}

	private static String intToString(int i){
		switch(i){
			case 1:
				return "一";
			case 2:
				return "二";
			case 3:
				return "三";
			case 4:
				return "四";
			case 5:
				return "五";
			case 6:
				return "六";
			case 7:
				return "日";
			default:
		}
		return "";
	}

	private static String arrayToString(List<String> workerList) {
		String workers = "";
		if(workerList==null || workerList.size()==0){
			return "";
		}
		for(String s:workerList){
			workers += s + ",";
		}
		if(workers.length()!=0){
			return workers.substring(0, workers.length()-1);
		}
		return "";
	}

	/**
	 * 进行每日排班
	 * @param finalSchedule 最终班次
	 * @param i 星期几
	 */
	private static void doEverySchedule(Map<String, Object> finalSchedule,int i) {
		int finalNum = -1;
		List<String> normalPerson = getNormalWorker();
		List<String> specialPerson = getSpecialWorker();
		//构造可以排班的人数
		List<String> workerList = new LinkedList<String>();
		workerList.addAll(normalPerson);
		workerList.addAll(specialPerson);
		//将休假的人员从workerList中剔除
		workerList = removeWorkerFromRestSchedule(workerList,finalSchedule,i);
		//循环班次名称列表，进行排班
		for(String scheduleName:getScheduleList()){
			//获取每个班次排班人数
			finalNum = getFinalWorkerNum(scheduleName,judgeIsWeekend(i));
			//剔除特殊排班的人数
			int hasScheduleNum = 0;
			if(scheduleName.equals(PRE_FLUID) || scheduleName.equals(MOVE)){
				hasScheduleNum = plusSpecialScheduleNum(scheduleName,i,finalSchedule,workerList);
			}
			finalNum = finalNum-hasScheduleNum;
			//排班人数小于或等于0，表示不进行排班
			if(finalNum<=0){
				continue;
			}
			Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
			if(scheduleMap==null || scheduleMap.isEmpty()){
				scheduleMap = new HashMap<String,List<String>>();
			}
			List<String> tempWorkerList = scheduleMap.get(scheduleName);
			if(tempWorkerList==null || tempWorkerList.isEmpty()){
				tempWorkerList = getScheduleWorker(workerList,finalNum,scheduleName,finalSchedule,i);
			}else{
				tempWorkerList.addAll(getScheduleWorker(workerList,finalNum,scheduleName,finalSchedule,i));
			}
			//生成每个班次排班人员，放入最终排班map
			scheduleMap.put(scheduleName, tempWorkerList);
			//放入最终的周排班map
			finalSchedule.put(String.valueOf(i), scheduleMap);
		}
		if(workerList.size()!=0){
			//周四特殊处理，全勤
			if(i==4){
				reScheduleWorkerThisDay(finalSchedule,workerList,i);
			}else{
				//如果排班人数有多，则进行放假
				//放假规则：一周最多2天，至少1天周末（则不能2天都是工作日）
				//Map<String,List<String>> restWorkerList = getlegalAndUnlegalRestWorker(finalSchedule,workerList,i);
				//List<String> unlegalWorker = restWorkerList.get("unlegalRestWorkerList");
				//List<String> legalWorker = restWorkerList.get("legalRestWorkerList");
				//if(unlegalWorker!=null && !unlegalWorker.isEmpty()){
					//将不该休息的人进行重新排班
					//reScheduleWorker(finalSchedule,unlegalWorker,i);
				//}
				
				System.out.println(i+"多余的排班人员:"+JSONArray.toJSONString(workerList));
				List<String> unlegalWorkerList = new ArrayList<String>();
				List<String> legalWorkerList = new ArrayList<String>();
				//循环多余人员列表，如果有休假等于2天，则重新排班
				for(String restWorker:workerList){
					if(getDaysOfAllSchedule(finalSchedule, DAY_REST, restWorker)==2){
						unlegalWorkerList.add(restWorker);
					}else{
						legalWorkerList.add(restWorker);
					}
				}
				//
				if(!unlegalWorkerList.isEmpty()){
					reScheduleWorkerToAllWeek(finalSchedule,unlegalWorkerList,i);
				}
				if(!legalWorkerList.isEmpty()){
					//将应该休息的人放入list中
					Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
					//scheduleMap.put(DAY_REST,legalWorker);
					List<String> restWorkerList = scheduleMap.get(DAY_REST);
					if(restWorkerList==null || restWorkerList.isEmpty()){
						restWorkerList = new ArrayList<String>();
					}
					restWorkerList.addAll(legalWorkerList);
					scheduleMap.put(DAY_REST,restWorkerList);
					finalSchedule.put(String.valueOf(i), scheduleMap);
				}
			}
			
		}
	}
	
	/**
	 * 将放假次数多的人员，重新放到本周去排班
	 * 两种情况：
	 * 1、如果当天非周末，把此人重新丢到当天去排班
	 * 2、如果当天为周末，则让他休息周末，随机找一个非周末的日期，把他重新排班
	 * @param finalSchedule
	 * @param unlegalWorkerList
	 */
	private static void reScheduleWorkerToAllWeek(Map<String, Object> finalSchedule, List<String> unlegalWorkerList,int i) {
		if(!judgeIsWeekend(i)){
			reScheduleWorkerThisDay(finalSchedule, unlegalWorkerList, i);
		}else{
			for(String workerName:unlegalWorkerList){
				String day = findAndRemoveFirstScheduleTypeFromScheduleMap(finalSchedule,workerName,DAY_REST);
				if(day.equals("-1")){
					continue;
				}
				List<String> tempList = new ArrayList<String>();
				tempList.add(workerName);
				reScheduleWorkerThisDay(finalSchedule, tempList, Integer.valueOf(day));
			}
		}
	}

	/**
	 * 找出第一次休息的班次的日期
	 * @param finalSchedule
	 * @param workerName
	 * @param dayRest
	 */
	private static String findAndRemoveFirstScheduleTypeFromScheduleMap(Map<String, Object> finalSchedule, String workerName,String scheduleType) {
		for(Entry<String, Object> entryObj:finalSchedule.entrySet()){
			Map<String,List<String>> entryMap = (Map<String, List<String>>) entryObj.getValue();
			if(entryMap==null || entryMap.isEmpty()){
				continue;
			}
			for(Entry<String, List<String>> entryList:entryMap.entrySet()){
				if(entryList.getKey().equals(scheduleType)){
					List<String> tempList = entryList.getValue();
					if(tempList==null || tempList.isEmpty()){
						continue;
					}
					if(tempList.contains(workerName)){
						tempList.remove(workerName);
						return entryObj.getKey();
					}
				}
			}
		}
		return "-1";
	}

	/**
	 * 判断某人在排班中某种类型的次数
	 * @return
	 */
	private static int getDaysOfAllSchedule(Map<String, Object> finalSchedule,String scheduleType,String worker){
		int i = 0;
		for(Entry<String, Object> entryObj:finalSchedule.entrySet()){
			Map<String,List<String>> entryMap = (Map<String, List<String>>) entryObj.getValue();
			if(entryMap==null || entryMap.isEmpty()){
				continue;
			}
			for(Entry<String, List<String>> entryList:entryMap.entrySet()){
				if(entryList.getKey().equals(scheduleType)){
					List<String> tempList = entryList.getValue();
					if(tempList==null || tempList.isEmpty()){
						continue;
					}
					if(tempList.contains(worker)){
						System.out.println(">>>>>>"+worker+"放假日期为:"+entryObj.getKey());
					}
					i += getCountOfList(tempList, worker);
				}
			}
		}
		System.out.println("====="+worker+"目前休假天数为:"+i);
		return i;
	}

	/**
	 * 将休假的人员从将要排班的列表人员中剔除
	 * @param workerList
	 * @param finalSchedule
	 * @param i
	 */
	private static List<String> removeWorkerFromRestSchedule(List<String> workerList, Map<String, Object> finalSchedule,int i) {
		List<String> resultList = new ArrayList<String>();
		Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
		if(scheduleMap==null || scheduleMap.isEmpty()){
			return workerList;
		}
		List<String> restWorkerList = scheduleMap.get(DAY_REST);
		if(restWorkerList==null || restWorkerList.isEmpty()){
			return workerList;
		}
		for(String worker:workerList){
			if(!restWorkerList.contains(worker)){
				resultList.add(worker);
			}
		}
		return resultList;
	}

	/**
	 * 获取已经排班的人数
	 * @param scheduleName 班次名称
	 * @param i 周几
	 * @param finalSchedule 排班
	 * @return
	 */
	private static int plusSpecialScheduleNum(String scheduleName, int i, Map<String, Object> finalSchedule,List<String> workerList) {
		Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
		if(scheduleMap==null || scheduleMap.isEmpty()){
			return 0;
		}
		List<String> list = scheduleMap.get(scheduleName);
		if(list==null || list.isEmpty()){
			return 0;
		}
		for(String name:list){
			if(workerList.contains(name)){
				workerList.remove(name);
			}
		}
		return list.size();
	}

	/**
	 * 将不该休息的休息的人进行重新排班
	 * 1、找出可以继续排班的班次（最少xx人这种）
	 * @param finalSchedule
	 * @param unlegalWorker
	 */
	private static void reScheduleWorkerThisDay(Map<String, Object> finalSchedule, List<String> unlegalWorker,int i) {
		Random rand = new Random();
		List<String> atleastScheduleNames = getAtLeastSchedules(i);
		if(atleastScheduleNames==null || atleastScheduleNames.isEmpty()){
			return;
		}
		Map<String,List<String>> schedulMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
		for(String workerName:unlegalWorker){
			if(atleastScheduleNames.size()==0){
				atleastScheduleNames = getAtLeastSchedules(i);
			}
			String randomScheduleName = atleastScheduleNames.remove((int)(Math.random()*atleastScheduleNames.size()));
			List<String> workerList = schedulMap.get(randomScheduleName);
			workerList.add(workerName);
			schedulMap.put(randomScheduleName, workerList);
		}
		finalSchedule.put(String.valueOf(i), schedulMap);
	}

	/**
	 * 获取排班可以进行最少人数排班的班次名称
	 * @param i
	 * @return
	 */
	private static List<String> getAtLeastSchedules(int i) {
		List resultList = new ArrayList();
		for(Entry<String, String> entry:scheduleTypes.entrySet()){
			String key = entry.getKey();
			String value = entry.getValue();
			if(i!=6 && i!=7){
				if("Z".equals((value.split("_")[0]).substring(0, 1))){
					resultList.add(key);
				}
			}else{
				if("Z".equals((value.split("_")[1]).substring(0, 1))){
					resultList.add(key);
				}
			}
		}
		return resultList;
	}

	/**
	 * 获取放假不合理人员，重新丢到该天的排班
	 * 放假规则：一周最多2天，至少1天周末（则不能2天都是工作日）
	 * @param finalSchedule
	 * @return
	 */
	private static Map<String,List<String>> getlegalAndUnlegalRestWorker(Map<String, Object> finalSchedule,List<String> workerList,int i) {
		List<String> unlegalWorker = new ArrayList<String>();
		List<String> legalWorker = new ArrayList<String>();
		List<String> restWorkerList = getWorkerListOfThisWeek(finalSchedule,DAY_REST,null);
		for(String workerName:workerList){
			int count = getCountOfList(restWorkerList,workerName);
			if(i!=6 && i!=7){
				if(count>=1){
					unlegalWorker.add(workerName);
				}else{
					legalWorker.add(workerName);
				}
			}else{
				if(count>=2){
					unlegalWorker.add(workerName);
				}else{
					legalWorker.add(workerName);
				}
			}
		}
		Map<String,List<String>> resultList = new HashMap<String,List<String>>();
		resultList.put("legalRestWorkerList", legalWorker);
		resultList.put("unlegalRestWorkerList", unlegalWorker);
		return resultList;
	}

	/**
	 * 获取某一类型班次本周排班人员
	 * @param finalSchedule 最终班次
	 * @param type 班次类型
	 * @param dayType 日期类型
	 * 				workday 工作日
	 * 				weekend 周末 
	 * @return
	 */
	private static List<String> getWorkerListOfThisWeek(Map<String, Object> finalSchedule,String type,String dayType) {
		List<String> resultList = new ArrayList<String>();
		if(finalSchedule==null || finalSchedule.isEmpty()){
			return resultList;
		}
		for(int i=1;i<=7;i++){
			Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
			if(scheduleMap==null || scheduleMap.isEmpty()){
				continue;
			}
			for(Entry<String, List<String>> entry:scheduleMap.entrySet()){
				if(entry.getKey().equals(type)){
					List<String> workerList = entry.getValue();
					if(workerList!=null && !workerList.isEmpty()){
						if(dayType==null){
							resultList.addAll(workerList);
						}else if(dayType.equals("workday") && (i!=6 && i!=7)){
							resultList.addAll(workerList);
						}else if(dayType.equals("weekend") && (i==6 || i==7)){
							resultList.addAll(workerList);
						}
					}
				}
			}
		}
		
		return resultList;
	}

	/**
	 * 判断是否为周末
	 * @param i
	 * @return
	 */
	private static boolean judgeIsWeekend(int i) {
		if(i==6 || i==7){
			return true;
		}
		return false;
	}

	public static int getFinalWorkerNum(String type,boolean isWeekend) {
		int finalNum = -1;
		Map<String,String> xzMap = getPersonNums(type,isWeekend);
		if(xzMap.get("type").equals("G")){
			finalNum = Integer.parseInt(xzMap.get("num"));
		}else{
			int leastNum = Integer.parseInt(xzMap.get("num"));
			finalNum = leastNum+(int)(Math.random()*2);
		}
		return finalNum;
	}
	
	/**
	 * 根据入参和班次，获取班次人员列表
	 * @param workerList 人员列表
	 * @param workerNum 随机提取的班次人员
	 * @param type 班次类型，控制某些人员不能进行提取
	 * @return
	 */
	private static List<String> getScheduleWorker(List<String> workerList,int workerNum,String type,Map<String, Object> finalSchedule,int i){
		List<String> resultList = new ArrayList<String>();
		while(resultList.size()<workerNum){
			System.out.println(i+"="+JSONArray.toJSONString(workerList)+"="+type);
			int randomIndex = (int)(Math.random()*workerList.size());
			String worker = workerList.get(randomIndex);
			//S1已经特殊排班，不再进行其他排班
			if("S1".equals(worker)){
				continue;
			}
			//S1人员不能进行如下班次
			if((type.equals(NEW_ADD) || type.equals(FORMULA) || type.equals(DRUG_TRANS) 
					|| type.equals(NIGHT_REST) || type.equals(FRONT)) && worker.equals("S1")){
				continue;
			}
			//S1本周进行过1次机动，不能再排机动
			if(type.equals(MOVE) && "S1".equals(worker)){
				List<String> moveWorkerList = getWorkerListOfThisWeek(finalSchedule,MOVE,null);
				if(moveWorkerList!=null && !moveWorkerList.isEmpty() && moveWorkerList.contains(worker)){
					continue;
				}
			}
			//S1本周进行过4个配液后，不能再排机动
			if(type.equals(PRE_FLUID) && "S1".equals(worker)){
				List<String> preFluidWorkerList = getWorkerListOfThisWeek(finalSchedule,PRE_FLUID,null);
				if(preFluidWorkerList!=null && !preFluidWorkerList.isEmpty() && getCountOfList(preFluidWorkerList,worker)==4){
					continue;
				}
			}
			//S1、S2、S3不能进行如下班次
			if(type.equals(NIGHT_REST) && (worker.equals("S1") || worker.equals("S2") || worker.equals("S3"))){
				continue;
			}
			//如果为夜休班次，需要对这周以往班次的人员进行筛选，不允许某人一周两次及以上夜休再次选择他进行夜休
			if(type.equals(NIGHT_REST)){
				List<String> nightRestWorkerList = getWorkerListOfThisWeek(finalSchedule,NIGHT_REST,null);
				if(nightRestWorkerList!=null && !nightRestWorkerList.isEmpty() && nightRestWorkerList.contains(worker)){
					continue;
				}
			}
			resultList.add(workerList.remove(randomIndex));
		}
		return resultList;
	}

	/**
	 * 获取字符串在一个list中出现的次数
	 * @param preFluidWorkerList
	 * @param worker
	 * @return
	 */
	private static int getCountOfList(List<String> workerList, String worker) {
		int i = 0;
		for(String name:workerList){
			if(worker.equals(name)){
				i++;
			}
		}
		return i;
	}

	/**
	 * 根据班次类型获取工作人数
	 * @param scheduleType 班次类型：新增、配方等
	 * @param isWeekend 是否周末
	 * @return
	 */
	private static Map<String,String> getPersonNums(String scheduleType,boolean isWeekend){
		String value = scheduleTypes.get(scheduleType).split("_")[0];
		if(isWeekend){
			value = scheduleTypes.get(scheduleType).split("_")[1];
		}
		Map<String,String> map = new HashMap<String,String>();
		map.put("type",value.substring(0,1));
		map.put("num",value.substring(1,2));
		return map;
	}

	/**
	 * 初始化班次map
	 * key:班次名称
	 * value:(G:固定，Z：最少)班次平日人数_(G:固定，Z：最少)班次周末人数
	 */
	private static void initSchedules() {
		scheduleTypes.put(NEW_ADD, "G1_G3");
		scheduleTypes.put(FORMULA, "Z2_G0");
		scheduleTypes.put(DRUG_TRANS, "G1_G0");
		scheduleTypes.put(MOVE, "Z1_G0");
		scheduleTypes.put(PRE_FLUID, "G2_G2");
		scheduleTypes.put(FRONT, "G1_G0");
		scheduleTypes.put(NIGHT_REST, "G1_G1");
	}
	
	/**
	 * 获取班次列表
	 * @return
	 */
	private static List<String> getScheduleList(){
		String[] arr = {NIGHT_REST,NEW_ADD,FORMULA,DRUG_TRANS,MOVE,PRE_FLUID,FRONT};
		return Arrays.asList(arr);
	}
	
	/**
	 * 获取班次列表
	 * @return
	 */
	private static List<String> getAllScheduleList(){
		String[] arr = {NIGHT_REST,NEW_ADD,FORMULA,DRUG_TRANS,MOVE,PRE_FLUID,FRONT,DAY_REST};
		return Arrays.asList(arr);
	}
	
	/**
	 * 获取普通上班人员
	 * 5天上班+2天休息
	 * @return
	 */
	private static List<String> getNormalWorker(){
		List<String> list = new LinkedList<String>();
		String arrays[] = {"A","B","C","D","E","F","G","H","I","J","K","L"};
		list = Arrays.asList(arrays);
		return list;
	}
	
	private static List<String> getWorkDayCanRest(){
		//生成休假星期
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("5");
		return list;
	}
	
	private static List<String> getWeekendDayCanRest(){
		List<String> list = new ArrayList<String>();
		list.add("6");
		list.add("7");
		return list;
	}
	
	/**
	 * 获取特殊上班人员
	 * 无夜休班次
	 * S1=4次配液+1次机动+2次休假（7天）
	 * S2、S3无夜休班次
	 * @return
	 */
	private static List<String> getSpecialWorker(){
		List<String> list = new LinkedList<String>();
		String arrays[] = {"S1","S2","S3"};
		list = Arrays.asList(arrays);
		return list;
	}
	
	/**
	 * 打印班次
	 * @param finalSchedule
	 */
	private static void printAllSchedule(Map<String, Object> finalSchedule) {
		List<String> scheduleList = getAllScheduleList();
		String schduleNamePrint = "";
		for(String schduleName:scheduleList){
			schduleNamePrint += schduleName + "                        ";
		}
		System.out.println("            " + schduleNamePrint);
		for(int i=1;i<=7;i++){
			String schedulePrintAll = "";
			String everyDayPrint = "";
			Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
			if(scheduleMap!=null && !scheduleMap.isEmpty()){
				for(String schduleName:scheduleList){
					List<String> workerList = scheduleMap.get(schduleName);
					everyDayPrint += "|"+arrayToString(workerList) + "|       ";
				}
			}
			schedulePrintAll += "星期"+i+" "+everyDayPrint;
			System.out.println(schedulePrintAll);
		}
	}
	
	public static void exportResult(Map<String, Object> finalSchedule,String finalXlsxPath) {
		OutputStream out = null;
		List<String> scheduleList = getAllScheduleList();
		try {
			// 获取总列数
			int columnNumCount = scheduleList.size()+1;
			// 读取Excel文档
			File finalXlsxFile = new File(finalXlsxPath);
			Workbook workBook = getWorkbok(finalXlsxFile);
			// sheet 对应一个工作页
			Sheet sheet = workBook.getSheetAt(0);
			//删除原有数据
			for (int i = 0; i < sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i+1);
				sheet.removeRow(row);
			}
			//创建文件输出流，输出电子表格：这个必须有，否则你在sheet上做的任何操作都不会有效
			out = new FileOutputStream(finalXlsxPath);
			workBook.write(out);
			//写入排班结果
			//先写titile
			Row rowTitle = sheet.createRow(0);
			for(int i=0;i<scheduleList.size();i++){
				Cell tempCell = rowTitle.createCell(i+1);
				tempCell.setCellValue(scheduleList.get(i));
			}
			//写班次数据
			for(int i=1;i<=7;i++){
				Row tempRow = sheet.createRow(i);
				Cell dayCell = tempRow.createCell(0);
				dayCell.setCellValue("星期"+intToString(i));
				Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
				if(scheduleMap!=null && !scheduleMap.isEmpty()){
					for(int j=0;j<scheduleList.size();j++){
						List<String> workerList = scheduleMap.get(scheduleList.get(j));
						String workers = arrayToString(workerList);
						Cell tempCell = tempRow.createCell(j+1);
						tempCell.setCellValue(workers);
					}
				}
			}
			//创建文件输出流，准备输出电子表格：这个必须有，否则你在sheet上做的任何操作都不会有效
			out = new FileOutputStream(finalXlsxPath);
			workBook.write(out);
			
			//----------------再输出一份--------------------------------
			Sheet sheet2 = workBook.getSheetAt(1);
			//删除原有数据
			for (int i = 0; i < sheet2.getLastRowNum(); i++) {
				Row row = sheet2.getRow(i+1);
				sheet2.removeRow(row);
			}
			//创建文件输出流，输出电子表格：这个必须有，否则你在sheet上做的任何操作都不会有效
			out = new FileOutputStream(finalXlsxPath);
			workBook.write(out);
			//写入排班结果
			//先写titile，名称
			Row rowTitle2 = sheet2.createRow(0);
			List<String> normalPerson = getNormalWorker();
			List<String> specialPerson = getSpecialWorker();
			List<String> totalWorker = new ArrayList<String>();
			totalWorker.addAll(normalPerson);
			totalWorker.addAll(specialPerson);
			for(int i=0;i<totalWorker.size();i++){
				Cell tempCell = rowTitle2.createCell(i+1);
				tempCell.setCellValue(totalWorker.get(i));
			}
			//写班次数据
			for(int i=1;i<=7;i++){
				Row tempRow = sheet2.createRow(i);
				Cell dayCell = tempRow.createCell(0);
				dayCell.setCellValue("星期"+intToString(i));
				Map<String,List<String>> scheduleMap = (Map<String, List<String>>) finalSchedule.get(String.valueOf(i));
				if(scheduleMap!=null && !scheduleMap.isEmpty()){
					for(int j=0;j<totalWorker.size();j++){
						String worker = totalWorker.get(j);
						for(Map.Entry<String, List<String>> smap:scheduleMap.entrySet()){
							List<String> workerList = smap.getValue();
							if(workerList.contains(worker)){
								Cell tempCell = tempRow.createCell(j+1);
								tempCell.setCellValue(smap.getKey());
								break;
							}
						}
					}
				}
			}
			//创建文件输出流，准备输出电子表格：这个必须有，否则你在sheet上做的任何操作都不会有效
			out = new FileOutputStream(finalXlsxPath);
			workBook.write(out);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("数据导出成功");
	}
	
	/**
	 * 判断Excel的版本,获取Workbook
	 * 只支持2007-2010版本
	 * @param in
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Workbook getWorkbok(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		Workbook wb = new XSSFWorkbook(in);
		return wb;
	}
	
}
