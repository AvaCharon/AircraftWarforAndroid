package com.example.serverforaw;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.sf.json.JSONObject;
import net.sf.json.JSONString;

//import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
    private static final String TAG = "Server";
    private String content = "";
    private DBHelper dbHelper;
    private SQLiteDatabase database;

    public  Server(){
        try{
            initDataBase(MyApplication.getContext());

            InetAddress addr = InetAddress.getLocalHost();
            System.out.println("local host:" + addr);

            //创建server socket
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("listen port 9999");

            while(true){
                System.out.println("waiting client connect");
                Socket socket = serverSocket.accept();
                System.out.println("accept client connect" + socket);
                new Thread(new LoginService(socket)).start();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    class LoginService implements Runnable{
        private Socket socket;
        private BufferedReader in = null;
        private Cursor cursor;
        public String message;

        public LoginService(Socket socket){
            this.socket = socket;
            try{
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
        //json对象解析
        public void parserJson(String content){
            System.out.println(content);
            JSONObject jsonObject = JSONObject.fromObject(content);
            if (jsonObject != null){
                String action = jsonObject.getString("action");
                String name = jsonObject.getString("name");
                String password = jsonObject.getString("password");
                cursor = database.query("User",null,null,null,null,null,null);
                //注册
                if("register".equals(action)){
                    //检查是否已存在该用户名
                    if(cursor.moveToFirst()){
                        do {
                            if(name.equals(cursor.getString(cursor.getColumnIndex("name")))){
                                message = "用户名已存在";
                                cursor.close();
                                return;
                            }
                        }while (cursor.moveToNext());
                    }
                    //将新的用户数据写入数据库
                    ContentValues values = new ContentValues();
                    values.put("name",jsonObject.getString("name"));
                    values.put("password",jsonObject.getString("password"));
                    message = "注册成功";
                    cursor.close();
                    return;
                }
                else {
                    if(cursor.moveToFirst()){
                        do {
                            if(name.equals(cursor.getString(cursor.getColumnIndex("name")))){
                                if(password.equals(cursor.getString(cursor.getColumnIndex("password")))){
                                    message = "登录成功";
                                    cursor.close();
                                    return;
                                }
                                else{
                                    message = "密码错误";
                                    cursor.close();
                                    return;
                                }
                            }
                        }while (cursor.moveToNext());
                    }
                }
            }
        }

        @Override
        public void run() {
            System.out.println("wait client message " );
            try {
                while ((content = in.readLine()) != null) {
                    //解析客户端发送过来的JSON字符串
                    parserJson(content);
                    if(content.equals("bye")){
                        System.out.println("disconnect from client,close socket");
                        socket.shutdownInput();
                        socket.shutdownOutput();
                        socket.close();
                    }else {
                        this.sendMessge(socket);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        public void sendMessge(Socket socket) {
            PrintWriter pout = null;
            try{
                System.out.println("messge to client:" + message);
                pout = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(),"utf-8")),true);
                pout.println(message);
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }

    public void initDataBase(Context context){
        dbHelper = new DBHelper(context,"User.db",null,1);
        database = dbHelper.getWritableDatabase();
    }
}
