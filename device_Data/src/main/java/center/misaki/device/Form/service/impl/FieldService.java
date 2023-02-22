package center.misaki.device.Form.service.impl;

import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.Form.dao.FieldMapper;
import center.misaki.device.Form.dto.FormStrucDto;
import center.misaki.device.Form.pojo.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Misaki
 */
@Service
@Slf4j
public class FieldService {
    
    private final FieldMapper fieldMapper;

    public FieldService(FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }

    /**
     * 校验字段改变，删除字段，不管增加字段，增加字段涉及到子表结构的改变,放在 StructureServiceImpl 里面
     * 基本逻辑：根据 提交的表单子表单 对 原表单的子表单 进行删除
     * @param originFields 原表单的子表单
     * @param subForms 提交的表单子表单
     */
    @Async
    @Transactional
    public void checkDeleteChange(List<Form.FormFieldsDto> originFields, List<FormStrucDto.SubFormDto> subForms){
        //获取原来的子表单id集合
        Set<String> fieldsId = originFields.stream()
                                         .map(Form.FormFieldsDto::getFieldsId)
                                         .flatMap(Collection::stream)
                                         .collect(Collectors.toSet());
        //因为前端传过来的表单是完整的，包括现存的所有数据，所以如果原来的子表单id不在传过来的表单中就表示已删除
        subForms.forEach(s->{
            List<FormStrucDto.FieldStrucDto> fieldStrucDtos = s.getFields();
            fieldStrucDtos.forEach(f->{
                if(f.getFieldId()!=null){//如果子表单还存在就移出删除列表
                    fieldsId.remove(f.getFieldId());
                }
            });
        });
        log.info("将要删除 {} 个字段",fieldsId.size());//剩余在列表里的子表单个数表示要删除的个数
        fieldsId.forEach(fieldMapper::deleteById);//剩余在列表里的子表单表示已不存在新的表单中，全部删除
        log.info("删除成功");
    }

    /**
     * 基本逻辑：根据提交的 新子表单数据 对 原子表单数据 进行数据更新
     * @param originFields 原子表单数据
     * @param subForms 提交的新子表单数据
     */
    @Async
    @Transactional
    public void checkModifyChange(List<Form.FormFieldsDto> originFields,List<FormStrucDto.SubFormDto> subForms){
        //获取原来子表单id 和 子表单数据 的Map集合
        Map<String, Field> originFieldMap = new HashMap<>();
        originFields.stream()
                    .map(Form.FormFieldsDto::getFieldsId)
                    .flatMap(Collection::stream)
                    .forEach(fieldId->{
            originFieldMap.put(fieldId,fieldMapper.selectById(fieldId));
        });
        int oral=0;
        int success=0;
        for (FormStrucDto.SubFormDto s : subForms) {
            List<FormStrucDto.FieldStrucDto> fieldStrucDtos = s.getFields();
            for (FormStrucDto.FieldStrucDto f : fieldStrucDtos) {
                //如果不是新添加的子表单才进行修改数据操作（因为新添加的子表单没有fieldId或者fieldId不在原子表单id列表里）
                if (f.getFieldId() != null && originFieldMap.containsKey(f.getFieldId())) {
                    //获取原子表单
                    Field field = originFieldMap.get(f.getFieldId());
                    //提交的子表单数据与原数据不一样时作出修改
                    if(!field.getTypeId().equals(f.getTypeId())||!field.getName().equals(f.getName())||!field.getDetailJson().equals(f.getDetailJson())){
                        oral++;
                        field.setTypeId(f.getTypeId());
                        field.setUpdateTime(LocalDateTime.now());
                        field.setName(f.getName());
                        field.setDetailJson(f.getDetailJson());
                        int i = fieldMapper.updateById(field);//将修改后的数据持久化到数据库
                        success+=i;
                    }
                }
            }
        }
        log.info("原表单需要修改 {} 个字段，实际修改 {} 个",oral,success);
    }
    
}
