package com.yiming.socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;


import com.alibaba.fastjson.JSON;
import com.yiming.entity.StudentReqData;
import com.yiming.entity.User;
import com.yiming.util.Constant;
import com.yiming.util.GetHttpSessionConfigurator;

@ServerEndpoint(value = "/websocket/studentList/{studentReqData}", configurator= GetHttpSessionConfigurator.class)//这里是一个类注解，告诉虚拟机该类被注解为一个WebSocket端点
public class StudentManager {
  //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static CopyOnWriteArraySet<StudentManager> webSocketSet = new CopyOnWriteArraySet<StudentManager>();
    private static ConcurrentMap<String,List<StudentManager>> webSocketMap = new  ConcurrentHashMap();
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    private HttpSession httpSession;

    private static List<User> users = new LinkedList<>();
    /**
     * 连接建立成功调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数
     */
    @OnOpen
    public void onOpen(@PathParam("studentReqData") String stuData  ,Session session, EndpointConfig config){
    	StudentReqData stuReqData = JSON.parseObject(stuData, StudentReqData.class);
        if(null == stuReqData || "".equals(stuReqData.getLiveRoomNum())) {
            System.out.println("连接失败");
            return;
        }
        this.session = session;
        if("0".equals(stuReqData.getIsStudent())) {
            if(!webSocketMap.containsKey(stuReqData.getLiveRoomNum())) {
                List<StudentManager> students = new LinkedList<StudentManager>();
                webSocketMap.put(stuReqData.getLiveRoomNum(), students);
            }
        } else {
            List<StudentManager> students = webSocketMap.get(stuReqData.getLiveRoomNum());
            students.add(this);
            webSocketMap.put(stuReqData.getLiveRoomNum(), students);
        }
//        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
    }
    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("studentReqData")String stuData ,Session session){
//        webSocketSet.remove(this);  //从set中删除
//        User user = (User) this.httpSession.getAttribute(Constant.USER);
//        users.remove(user);
    	StudentReqData stuReqData = JSON.parseObject(stuData, StudentReqData.class);
        if("0".equals(stuReqData.getIsStudent())) {
            webSocketMap.remove(stuReqData.getLiveRoomNum());
        } else {
            List<StudentManager> students = webSocketMap.get(stuReqData.getLiveRoomNum());
            students.remove(this);
        }
        webSocketSet.remove(this);
        subOnlineCount();           //在线数减1
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        String retMessage = "";
        if(users.size() != 0) {
            retMessage = JSON.toJSONString(users);
        }
        //群发消息
        for(StudentManager item: webSocketSet){
            try {
                item.sendMessage(retMessage);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }
    /**
     * 发生错误时调用
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error){
        error.printStackTrace();
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException{
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        StudentManager.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        StudentManager.onlineCount--;
    }
}
