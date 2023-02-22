package center.misaki.device.Form.service;

import center.misaki.device.Form.dto.BatchChangeDto;
import center.misaki.device.Form.dto.BatchDeleteDto;
import center.misaki.device.Form.dto.DataScreenDto;
import center.misaki.device.Form.dto.OneDataDto;
import center.misaki.device.Form.pojo.FormData;
import center.misaki.device.Form.vo.BatchLogVo;
import center.misaki.device.Form.vo.FormDataVo;
import center.misaki.device.Form.vo.OneDataVo;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 表单数据操作实现类
 */
public interface FormService {
    
    //获得一张表中所有的数据，无区分( 目前软删除去除了，所以这个函数后下面的一个函数只存在流程数据的区别上 )
    FormDataVo getOneFormAllData(int formId,String userInfo);
    
    //获得一张表中正常的数据，区分其是否删除，和是否是流程产生的中间数据
    FormDataVo getOneFormData(int formId,String userInfo);
    
    //获得一张表中仅仅只有自己提交的数据，区分其是否删除，是否是流程产生的中间数据
    FormDataVo getOneUserFormData(int formId,String userInfo);
    
    //获得一条数据的详细信息,附加日志信息
    OneDataVo getOneData(int formId,int dataId,String userInfo);
    
    //获得一张表中的详细批量修改日志
    List<BatchLogVo> getBatchLogs(int formId,String userInfo);
    
    
    //添加一条数据,可能是需要流转的数据
    boolean addOneData(OneDataDto oneDataDto,String userInfo);
    
    //带有重复数据检查添加一条数据
    List<String> addOneData(OneDataDto.OneDataDtoPlus oneDataDtoPlus,String userInfo);
    
    //修改一条数据
    boolean changeOneData(OneDataDto oneDataDto,String userInfo);
    
    //带有重复校验去修改一条数据
    List<String> changeOneData(OneDataDto.OneDataDtoPlus oneDataDtoPlus,String userInfo);
    
    
    //批量修改数据
    void batchChangeData(BatchChangeDto batchChangeDto,String userInfo);
    
    //软删除一条数据,目前实现是实际删除
    void deleteOneData(Integer dataId,Integer formId,String userInfo) throws SQLException;
    
    //批量软删除数据，目前为了方便就实际删除
    void batchDelete(BatchDeleteDto batchChangeDto, String userInfo);
    
    //获得筛选后的全部数据
    FormDataVo getAllDataAfterScreen(DataScreenDto dataScreenDto,String userInfo) throws ExecutionException, InterruptedException;
    
    //获得本人提交后筛选的全部数据
    FormDataVo getUserDataAfterScreen(DataScreenDto dataScreenDto,String userInfo) throws ExecutionException, InterruptedException;
    
    
    //数据联动获取一条数据
    List<OneDataVo.OneFieldValue> dataLink(List<DataScreenDto> dataScreenDtos,String userInfo) throws ExecutionException, InterruptedException;
    
    //更改表单类型为流程表单
    boolean changeFormTypeToFlow(int formId,String userInfo);
    
    //更改表单类型为普通表单
    boolean changeFormTypeToNormal(int formId,String userInfo);
    
    List<OneDataVo.OneFormLinkValue> getDataLinkFormSearch(List<DataScreenDto> dataScreenDto, String userInfo) throws ExecutionException, InterruptedException;
    //快速查询数据
    List<FormData> fastQuery(List<Integer> dataIds);

}
