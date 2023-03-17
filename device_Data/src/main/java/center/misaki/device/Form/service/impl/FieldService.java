package center.misaki.device.Form.service.impl;

import center.misaki.device.domain.Pojo.Form;
import center.misaki.device.Form.dao.FieldMapper;
import center.misaki.device.Form.dto.FormStrucDto;
import center.misaki.device.Form.pojo.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class FieldService  extends ServiceImpl<FieldMapper,Field> implements IService<Field> {
    
    private final FieldMapper fieldMapper;

    public FieldService(FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }

    //校验字段改变，删除字段，不管增加字段，增加字段涉及到子表结构的改变,放在 StructureServiceImpl 里面
    @Async
    @Transactional
    public void checkDeleteChange(List<Form.FormFieldsDto> originFields, List<FormStrucDto.SubFormDto> subForms){
        Set<String> fields = originFields.stream().map(Form.FormFieldsDto::getFieldsId).flatMap(Collection::stream).collect(Collectors.toSet());
        subForms.forEach(s->{
            List<FormStrucDto.FieldStrucDto> fieldStrucDtos = s.getFields();
            fieldStrucDtos.forEach(f->{
                if(f.getFieldId()!=null){
                    fields.remove(f.getFieldId());
                }
            });
        });
        log.info("将要删除 {} 个字段",fields.size());
        fields.forEach(fieldMapper::deleteById);
        log.info("删除成功");
    }
    
    @Async
    @Transactional
    public void checkModifyChange(List<Form.FormFieldsDto> originFields,List<FormStrucDto.SubFormDto> subForms){
        Map<String, Field> originFieldMap = new HashMap<>();
        originFields.stream().map(Form.FormFieldsDto::getFieldsId).flatMap(Collection::stream).forEach(o->{
            originFieldMap.put(o,fieldMapper.selectById(o));
        });
        int oral=0;
        int success=0;
        for (FormStrucDto.SubFormDto s : subForms) {
            List<FormStrucDto.FieldStrucDto> fieldStrucDtos = s.getFields();
            for (FormStrucDto.FieldStrucDto f : fieldStrucDtos) {
                if (f.getFieldId() != null && originFieldMap.containsKey(f.getFieldId())) {
                    Field field = originFieldMap.get(f.getFieldId());
                    if(!field.getTypeId().equals(f.getTypeId())||!field.getName().equals(f.getName())||!field.getDetailJson().equals(f.getDetailJson())){
                        oral++;
                        field.setTypeId(f.getTypeId());
                        field.setUpdateTime(LocalDateTime.now());
                        field.setName(f.getName());
                        field.setDetailJson(f.getDetailJson());
                        int i = fieldMapper.updateById(field);
                        success+=i;
                    }
                }
            }
        }
        log.info("原表单需要修改 {} 个字段，实际修改 {} 个",oral,success);
    }

}
