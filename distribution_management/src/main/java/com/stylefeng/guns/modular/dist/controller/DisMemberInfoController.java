package com.stylefeng.guns.modular.dist.controller;

import com.google.gson.Gson;
import com.stylefeng.guns.common.controller.BaseController;
import com.stylefeng.guns.common.exception.BizExceptionEnum;
import com.stylefeng.guns.common.exception.BussinessException;
import com.stylefeng.guns.common.persistence.model.DisMemberInfo;
import com.stylefeng.guns.common.persistence.model.User;
import com.stylefeng.guns.modular.dist.service.IDisMemberAmountMongoService;
import com.stylefeng.guns.modular.dist.service.IDisMemberInfoService;
import com.stylefeng.guns.modular.dist.util.Jwt;
import com.stylefeng.guns.modular.dist.vo.Categories;
import com.stylefeng.guns.modular.dist.vo.DisMemberInfoVo;
import com.stylefeng.guns.modular.dist.wapper.MemberWarpper;
import com.stylefeng.guns.modular.system.dao.UserMgrDao;
import com.stylefeng.guns.modular.system.service.ISysDicService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 会员表控制器
 *
 * @author huangpu
 * @Date 2018-04-05 21:49:44
 */
@Controller
@RequestMapping("/disMemberInfo")
public class DisMemberInfoController extends BaseController {

    private String PREFIX = "/dist/disMemberInfo/";

    @Autowired
    IDisMemberInfoService  disMemberInfoService;

    @Autowired
    UserMgrDao userMgrDao;

    @Autowired
    ISysDicService sysDicService;

    @Autowired
    IDisMemberAmountMongoService disMemberAmountService;

    @Value("${dist.jwt.secret}")
    private  String secret;
    @Value("${dist.jwt.account}")
    private  String account;



    /**
     * 跳转到分销首页
     */
    @RequestMapping("")
    public String index() {
        return PREFIX + "disMemberInfo.html";
    }

    /**
     * 跳转到添加分销
     */
    @RequestMapping("/disMemberInfo_add")
    public String disMemberInfoAdd() {
        return PREFIX + "disMemberInfo_add.html";
    }

    /**
     * 跳转到修改分销
     */
    @RequestMapping("/disMemberInfo_update/{disMemberInfoId}")
    public String disMemberInfoUpdate(@PathVariable Integer disMemberInfoId, Model model) {
        return PREFIX + "disMemberInfo_edit.html";
    }
    @RequestMapping("/view/{id}")
    public String memberView(HttpServletRequest request, @PathVariable String id, Model model) {

        String[] detailInfo=disMemberInfoService.getDetaiCanvas(id);
        model.addAttribute("node",detailInfo[0]);
        model.addAttribute("link",detailInfo[1]);
        request.setAttribute("node",detailInfo[0]);
        request.setAttribute("link",detailInfo[1]);
        Map<String,Object> selecteds=new LinkedHashMap<>();
        List<String> datas=new ArrayList<>();
        List<Map<String, Object>> list=  sysDicService.selectListByCode("disUserType");
        List<String> colors= Arrays.asList("#ff7f50","#87cefa","#da70d6","#32cd32","#6495ed",
                "#ff69b4","#ba55d3","#cd5c5c","#ffa500","#40e0d0",
                "#1e90ff","#ff6347","#7b68ee","#00fa9a","#ffd700",
                "#6699FF","#ff6666","#3cb371","#b8860b","#30e0e0");
        String listCategories="[";
        if(list.size()>0){
            for (int i=0;i<list.size();i++){
                Categories categories=new Categories();
                categories.setColor(colors.get(i));
                categories.setName((String) list.get(i).get("dicValue"));
                listCategories=listCategories+categories.toString();
                if(i!=list.size()-1){
                    listCategories+=",";
                }
                selecteds.put(list.get(i).get("dicValue").toString(),true);
                datas.add(list.get(i).get("dicValue").toString());
            }
        }
        listCategories+="]";
        Gson gson=new Gson();
        request.setAttribute("listCategories",listCategories);
        request.setAttribute("selecteds",gson.toJson(selecteds));
        request.setAttribute("datas",gson.toJson(datas));
        return PREFIX + "detail.html";
    }

    /**
     * 获取分销列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(String condition) {
        List<Map<String, Object>>  list=disMemberInfoService.selectList();
        return super.warpObject(new MemberWarpper(list));
    }

    /**
     * 新增分销
     */
    @PostMapping(value = "/add")
    @ResponseBody
    @ApiOperation(value = "新增会员", notes = "")
    public Object add(DisMemberInfoVo memberInfoVo) {
        User user=userMgrDao.getByAccount(memberInfoVo.getDisPlatSuper());
        if(user==null){
            throw  new BussinessException(BizExceptionEnum.USER_NOT_EXISTED);
        }
        if(StringUtils.isNotEmpty(memberInfoVo.getDisModelId())){
            DisMemberInfo param= disMemberInfoService.selectListByUserId(memberInfoVo.getDisModelId());
            if(param==null){
                throw  new BussinessException(BizExceptionEnum.USERMEM_NOT_EXISTED);
            }
        }
        DisMemberInfo param= disMemberInfoService.selectListByUserId(memberInfoVo.getDisUserId());
        if(param!=null){
            throw  new BussinessException(BizExceptionEnum.USER_IS_EXISTED);
        }
        String acc= Jwt.unsign(memberInfoVo.getSecret(),secret,String.class);
        if(acc.equals(account)){
            DisMemberInfo memberInfo=new DisMemberInfo();
            BeanUtils.copyProperties(memberInfoVo,memberInfo);
            memberInfo.setDisUserType("0");
            memberInfo.setDisPlatSuper(memberInfoVo.getDisPlatSuper());
            memberInfo.setDisPlatLevel(Integer.parseInt(user.getLevel()));
            memberInfo.setDisPlatFullIndex(user.getFullindex());
            memberInfo.setDisPlatformId(user.getFullindex().split("\\.")[1]);
            disMemberInfoService.save(memberInfo);
            //disMemberAmountService.save(memberInfo.getDisUserId(),memberInfo.getDisUserName(),memberInfo.getDisPlatformId(),"1");
        }else {
            throw new BussinessException(BizExceptionEnum.ILLEGAL_INFO);
        }
        return super.SUCCESS_TIP;
    }

    /**
     * 删除分销
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public Object delete() {
        return SUCCESS_TIP;
    }


    /**
     * 修改分销
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public Object update() {
        return super.SUCCESS_TIP;
    }

    /**
     * 分销详情
     */
    @RequestMapping(value = "/detail")
    @ResponseBody
    public Object detail() {
        return null;
    }
}
