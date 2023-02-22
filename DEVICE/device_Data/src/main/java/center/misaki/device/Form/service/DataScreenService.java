package center.misaki.device.Form.service;

import center.misaki.device.Enum.ConditionsEnum;
import center.misaki.device.Enum.FieldTypeEnum;
import center.misaki.device.Form.dto.DataScreenDto;
import center.misaki.device.Form.pojo.FormData;
import center.misaki.device.Form.service.impl.FormDataService;
import center.misaki.device.utils.LocalDateTimeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Misaki
 * 数据筛选服务
 */
@Service
@Slf4j
public class DataScreenService {
    
    private final AsyncTaskExecutor executorService;
    private final FormDataService formDataService;
    
    public DataScreenService(AsyncTaskExecutor executorService, FormDataService formDataService) {
        this.executorService = executorService;
        this.formDataService = formDataService;
    }


    /**
     * 仅限表格筛选数据，同时亦可用于数据联动等等 
     */
    public List<FormData> screen(DataScreenDto dataScreenDto,List<FormData> originData) throws ExecutionException, InterruptedException {
        //获取表单使用的所有数据
        List<Map<String,String>> dataMap = formDataService.converterDataMap(originData);
        //筛选条件
        String restrictType = dataScreenDto.getRestrictType();
        //默认为和
        boolean and = restrictType == null || !restrictType.equals("or");
        List<DataScreenDto.FieldRestrict> conditions = dataScreenDto.getConditions();
        //如果所有条件都不设置，则返回所有数据
        if((conditions==null||conditions.isEmpty())&&dataScreenDto.getCreateTime()==null&&dataScreenDto.getCreatePerson()==null&&dataScreenDto.getUpdateTime()==null) return originData;
        List<Future<Set<Integer>>> futures = new ArrayList<>();
        //多线程筛选
        assert conditions != null;
        for(DataScreenDto.FieldRestrict restrict:conditions){
            //做出一点处理
            if(restrict.getCustom()!=null&&!restrict.getCustom()){
                restrict.setOperand(dataScreenDto.getNowValue().get(restrict.getOperand()));
            }
            futures.add(executorService.submit(()->screenFields(dataMap,restrict.getFieldId(),restrict.getFieldTypeId(),restrict.getOperand(),ConditionsEnum.operatorTo(restrict.getOperator()))));
        }
        if(dataScreenDto.getCreatePerson()!=null){
            futures.add(executorService.submit(()->screenPerson(originData,dataScreenDto.getCreatePerson().getOperand()
                    ,ConditionsEnum.operatorTo(dataScreenDto.getCreatePerson().getOperator()))));
        }
        if(dataScreenDto.getCreateTime()!=null){
            futures.add(executorService.submit(()->screenCreateOrUpdateTime(originData,dataScreenDto.getCreateTime().getOperand()
                    ,ConditionsEnum.operatorTo(dataScreenDto.getCreateTime().getOperator()),true)));
        }
        if(dataScreenDto.getUpdateTime()!=null){
            futures.add(executorService.submit(()->screenCreateOrUpdateTime(originData,dataScreenDto.getUpdateTime().getOperand()
                    ,ConditionsEnum.operatorTo(dataScreenDto.getUpdateTime().getOperator()),false)));
        }
        Set<Integer> ans = new HashSet<>();
        Iterator<Future<Set<Integer>>> iterator = futures.iterator();
        if(iterator.hasNext()) ans=iterator.next().get();
        while(iterator.hasNext()){
            Future<Set<Integer>> future = iterator.next();
            if(and) ans.retainAll(future.get());
            else ans.addAll(future.get());
        }
        log.info("原数据 {} 条筛选得到了 {} 条",originData.size(),ans.size());
        List<FormData> formData = new ArrayList<>(ans.size());
        ans.forEach(a->formData.add(originData.get(a)));
        return formData;
    }
    
    
    
    public Set<Integer> screenFields(List<Map<String,String>> originData, String fieldId, Integer fieldTypeId,String value,ConditionsEnum conditionsEnum){
        if(conditionsEnum==ConditionsEnum.EQUALS) return screenEqual(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.NO_EQUALS) return screenNoEqual(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.NO_CONTAINS) return screenNoContain(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.CONTAINS) return screenContain(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.EQUALS_ANYONE) return screenEqualAnyOne(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.NO_EQUALS_ANYONE) return screenNoEqualAnyOne(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.NULL) return screenNull(originData,fieldId);
        if(conditionsEnum==ConditionsEnum.NO_NULL) return screenNoNull(originData,fieldId);
        if(conditionsEnum==ConditionsEnum.GREATER) return screenGrater(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.GREATER_AND_EQUALS) return screenGraterAndEqual(originData,fieldId,fieldTypeId,value);
        if(conditionsEnum==ConditionsEnum.LESS) return screenLess(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.LESS_AND_EQUALS) return screenLessAndEqual(originData,fieldId,fieldTypeId,value);
        if(conditionsEnum==ConditionsEnum.SCOPE) return screenScope(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.DYNAMIC) return Dynamic(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.CONTAINS_ANYONE) return containAnyOne(originData,fieldId,value);
        if(conditionsEnum==ConditionsEnum.CONTAINS_ALL) return containAll(originData,fieldId,value);
        return new HashSet<>();
    }
    
    
    
    
    
    //筛选 等于号
    private Set<Integer> screenEqual(List<Map<String,String>> originData, String fieldId, String value){
        Set<Integer> ans = new HashSet<>();
        for(int i=0;i<originData.size();i++){
            Map<String,String> map = originData.get(i);
            if(!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(originValue.equals(value)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选等于，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    
    //筛选 不等于
    private Set<Integer> screenNoEqual(List<Map<String,String>> originData, String fieldId, String value){
        Set<Integer> ans = new HashSet<>();
        for(int i=0;i<originData.size();i++){
            Map<String,String> map = originData.get(i);
            if(!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(!originValue.equals(value)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选不等于，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    
    //筛选 等于任意一个
    private Set<Integer> screenEqualAnyOne(List<Map<String,String>> originData, String fieldId,String value){
        Set<String> values = JSON.parseObject(value, new TypeReference<Set<String>>() {});
        Set<Integer> ans = new HashSet<>();
        for(int i=0;i<originData.size();i++){
            Map<String,String> map = originData.get(i);
            if(!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(values.contains(originValue)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选等于任意一个，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    
    //筛选 不等于任意一个
    private Set<Integer> screenNoEqualAnyOne(List<Map<String,String>> originData, String fieldId,String value){
        Set<String> values = JSON.parseObject(value, new TypeReference<Set<String>>() {});
        Set<Integer> ans=new HashSet<>();
        for(int i=0;i<originData.size();i++){
            Map<String,String> map = originData.get(i);
            if(!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(!values.contains(originValue)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选不等于任意一个，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    
    //筛选包含 
    private Set<Integer> screenContain(List<Map<String,String>> originData, String fieldId,String value){
        Set<Integer> ans=new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(originValue.contains(value)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选包含，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    //筛选不包含
    private Set<Integer> screenNoContain(List<Map<String,String>> originData, String fieldId,String value){
        Set<Integer> ans=new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(!originValue.contains(value)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选不包含，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    //筛选为空
    private Set<Integer> screenNull(List<Map<String,String>> originData, String fieldId){
        Set<Integer> ans=new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)||map.get(fieldId).equals("")){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选为空的条件完毕",fieldId);
        return ans;
    }
    
    //筛选不为空
    private Set<Integer> screenNoNull(List<Map<String,String>> originData, String fieldId){
        Set<Integer> ans=new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (map.containsKey(fieldId)&&!map.get(fieldId).equals("")){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选不为空的条件完毕",fieldId);
        return ans;
    }
    
    //筛选大于
    private Set<Integer> screenGrater(List<Map<String,String>> originData, String fieldId,String value){
        Set<Integer> ans = new HashSet<>();
        Double d= Double.valueOf(value);
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)) continue;
            Double s = Double.valueOf(map.get(fieldId));
            if(d.compareTo(s)>0){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选大于，值{}的条件完毕",fieldId,value);
        return ans;
    }

    //筛选小于
    private Set<Integer> screenLess(List<Map<String,String>> originData, String fieldId,String value){
        Set<Integer> ans = new HashSet<>();
        Double d= Double.valueOf(value);
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)) continue;
            Double s = Double.valueOf(map.get(fieldId));
            if(s.compareTo(d)>0){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选小于，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    //筛选 大于等于
    private Set<Integer> screenGraterAndEqual(List<Map<String,String>> originData, String fieldId,Integer fieldTypeId,String value){
        Set<Integer> ans = new HashSet<>();
        if(fieldTypeId.equals(FieldTypeEnum.FIGURES.ordinal())){
            Double d= Double.valueOf(value);
            for(int i=0;i<originData.size();i++) {
                Map<String,String> map = originData.get(i);
                if (!map.containsKey(fieldId)) continue;
                Double s = Double.valueOf(map.get(fieldId));
                if(d.compareTo(s)>=0){
                    ans.add(i);
                }
            }
            log.info("对字段id为{} 筛选大于等于，值{}的条件完毕",fieldId,value);
            return ans;
        }
        if(fieldTypeId.equals(FieldTypeEnum.DATE_AND_TIME.ordinal())){
            LocalDateTime valueDate = LocalDateTimeUtil.parse(value);
            for(int i=0;i<originData.size();i++) {
                Map<String,String> map = originData.get(i);
                if (!map.containsKey(fieldId)||map.get(fieldId).equals("")) continue;
                LocalDateTime originDate=LocalDateTimeUtil.parse(map.get(fieldId));
                if(valueDate.isAfter(originDate)|| valueDate.isEqual(originDate)){
                    ans.add(i);
                }
            }
            log.info("对字段id为{} 筛选大于等于，值{}的条件完毕",fieldId,value);
            return  ans;
        }
        
        return ans;
    }
    
    //筛选  小于等于
    private Set<Integer> screenLessAndEqual(List<Map<String,String>> originData, String fieldId,Integer fieldTypeId,String value){
        Set<Integer> ans = new HashSet<>();
        if(fieldTypeId.equals(FieldTypeEnum.FIGURES.ordinal())){
            Double d= Double.valueOf(value);
            for(int i=0;i<originData.size();i++) {
                Map<String,String> map = originData.get(i);
                if (!map.containsKey(fieldId)) continue;
                Double s = Double.valueOf(map.get(fieldId));
                if(s.compareTo(d)>=0){
                    ans.add(i);
                }
            }
            log.info("对字段id为{} 筛选小于等于，值{}的条件完毕",fieldId,value);
            return ans;
        }
        if(fieldTypeId.equals(FieldTypeEnum.DATE_AND_TIME.ordinal())){
            LocalDateTime valueDate = LocalDateTimeUtil.parse(value);
            for(int i=0;i<originData.size();i++) {
                Map<String,String> map = originData.get(i);
                if (!map.containsKey(fieldId)||map.get(fieldId).equals("")) continue;
                LocalDateTime originDate=LocalDateTimeUtil.parse(map.get(fieldId));
                if(originDate.isAfter(valueDate)|| valueDate.isEqual(originDate)){
                    ans.add(i);
                }
            }
            log.info("对字段id为{} 筛选小于等于，值{}的条件完毕",fieldId,value);
            return  ans;
        }
        return ans;
    }
    
    //筛选 选择范围
    private Set<Integer> screenScope(List<Map<String,String>> originData, String fieldId,String value){
        List<LocalDateTime> timeScope = JSON.parseObject(value, new TypeReference<List<LocalDateTime>>() {});
        Set<Integer> ans = new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)||map.get(fieldId).equals("")) continue;
            LocalDateTime originDate = LocalDateTimeUtil.parse(map.get(fieldId));
            if(originDate.isAfter(timeScope.get(0))&&originDate.isBefore(timeScope.get(1))){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选选择范围，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    
    //筛选 动态筛选
    private Set<Integer> Dynamic(List<Map<String,String>> originData, String fieldId,String value){
        return screenScope(originData,fieldId,value);
    }
    
    //筛选 包含任意一个
    private Set<Integer> containAnyOne(List<Map<String,String>> originData, String fieldId,String value){
        List<String> values = JSON.parseObject(value, new TypeReference<List<String>>() {});
        HashSet<Integer> ans = new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(values.stream().anyMatch(originValue::contains)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选包含任意一个，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    //筛选 同时包含
    private Set<Integer> containAll(List<Map<String,String>> originData, String fieldId,String value){
        List<String> values = JSON.parseObject(value, new TypeReference<List<String>>() {});
        HashSet<Integer> ans = new HashSet<>();
        for(int i=0;i<originData.size();i++) {
            Map<String,String> map = originData.get(i);
            if (!map.containsKey(fieldId)) continue;
            String originValue = map.get(fieldId);
            if(values.stream().allMatch(originValue::contains)){
                ans.add(i);
            }
        }
        log.info("对字段id为{} 筛选同时包含，值{}的条件完毕",fieldId,value);
        return ans;
    }
    
    
    //特殊， 筛选提交人
    public Set<Integer> screenPerson(List<FormData> formData, String person, ConditionsEnum conditionsEnum){
        Set<Integer> ans = new HashSet<>();
        
        Set<String> persons=null;
        for(int i=0;i<formData.size();i++){
            String createPerson = formData.get(i).getCreatePerson();
            switch (conditionsEnum){
                case EQUALS:
                    if(createPerson.equals(person)) ans.add(i);
                    break;
                case NO_EQUALS:
                    if(!createPerson.equals(person)) ans.add(i);
                    break;
                case EQUALS_ANYONE:
                    persons=persons==null?JSON.parseObject(person, new TypeReference<Set<String>>() {}):persons;
                    if(persons.contains(createPerson)){
                        ans.add(i);
                    }
                    break;
                case NO_EQUALS_ANYONE:
                    persons=persons==null?JSON.parseObject(person, new TypeReference<Set<String>>() {}):persons;
                    if(!persons.contains(createPerson)){
                        ans.add(i);
                    }
                    break;
                case NULL:
                    if(createPerson==null||createPerson.equals("")) ans.add(i);
                    break;
                case NO_NULL:
                    if(createPerson!=null&&!createPerson.equals("")) ans.add(i);
                    break;
            }
        }
        log.info("对提交人筛选，值{}的条件完毕",person);
        return ans;
    }
    
    //特殊 筛选提交时间和更新时间
    public Set<Integer> screenCreateOrUpdateTime(List<FormData> formData, String dateTime, ConditionsEnum conditionsEnum,boolean isCreate){
        Set<Integer> ans = new HashSet<>();
        LocalDateTime value=null;
        List<LocalDateTime> scope=null;
        for(int i=0;i<formData.size();i++){
            LocalDateTime time;
            if(isCreate){
               time=formData.get(i).getCreateTime();
            }else{
               time=formData.get(i).getUpdateTime();
            }
            switch (conditionsEnum){
                case EQUALS:
                    value=value==null? LocalDateTimeUtil.parse(dateTime):value;
                    if(value.equals(time)) ans.add(i);
                    break;
                case NO_EQUALS:
                    value=value==null? LocalDateTimeUtil.parse(dateTime):value;
                    if(!value.equals(time)) ans.add(i);
                    break;
                case NULL:
                    if(time==null) ans.add(i);
                    break;
                case NO_NULL:
                    if(time!=null) ans.add(i);
                    break;
                case GREATER_AND_EQUALS:
                    value=value==null? LocalDateTimeUtil.parse(dateTime):value;
                    if(time.isAfter(value)||value.equals(time)) ans.add(i);
                    break;
                case LESS_AND_EQUALS:
                    value=value==null? LocalDateTimeUtil.parse(dateTime):value;
                    if(time.isBefore(value)||value.equals(time)) ans.add(i);
                    break;
                case SCOPE:
                case DYNAMIC:
                    if(scope==null){
                        List<String> times = JSON.parseObject(dateTime, new TypeReference<List<String>>() {});
                        scope=times.stream().map(LocalDateTimeUtil::parse).collect(Collectors.toList());
                    }
                    if(time.isAfter(scope.get(0))&&time.isBefore(scope.get(1))) ans.add(i);
                    break;
            }
        }
        log.info("对提交时间或更新时间筛选，值{}的条件完毕",dateTime);
        return ans;
    }
}
