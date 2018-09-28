using System;
using System.Collections.Generic;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ConsoleApp1
{
    class Program
    {
        static SerialPort device;
        static Int16 maxSpeed = 2500;
        static Int16 curSpeed = 500;

        /// <summary>
        /// 位置（旋转 角度，单位0.1度）
        /// </summary>
        static Int16 minPos1 = -1800;
        static Int16 maxPos1 = 1800;
        static Int16 curPos1 = 0;

        static Int16 minPos2 = -1800;
        static Int16 maxPos2 = 1800;
        static Int16 curPos2 = 0;

        static Int16 minPos3 = -1800;
        static Int16 maxPos3 = 1800;
        static Int16 curPos3 = 0;

        static Int16 minPos4 = -1800;
        static Int16 maxPos4 = 1800;
        static Int16 curPos4 = 0;

        static Int16 minPos5 = -1800;
        static Int16 maxPos5 = 1800;
        static Int16 curPos5 = 0;

        static Int16 minPos6 = -1800;
        static Int16 maxPos6 = 1800;
        static Int16 curPos6 = 0;
        
        static void Main(string[] args)
        {
            device = new SerialPort();

            if (!open())
                return;

            Thread recvThread = new Thread(receivedata);
            recvThread.Start();

            Thread updateThread = new Thread(updateInfo);
            updateThread.Start();

            while (true)
            {
                Thread.Sleep(1000);
            }
        }

        private static bool open()
        {
            if (device.IsOpen)
            {
                Console.WriteLine("串口已打开！");
                return true;
            }

            String[] names = SerialPort.GetPortNames();
            if (names == null || names.Length <= 0)
            {
                Console.WriteLine("未发现任何串口，请检查线路连接是否正常！");
                Console.ReadKey();
                return false;
            }

            int index = 0;
            foreach (String item in names)
            {
                Console.WriteLine(String.Format("{0}:{1}", ++index, item));
            }
            Console.WriteLine(String.Format("请选择（1-{0}）:", names.Length));
            String value = Console.ReadLine();
            if (Int32.TryParse(value, out index))
            {
                if (index > 0 && index <= names.Length + 1)
                value = names[index - 1];
            }
            // 开始打开串口

            device.PortName = value;
            device.BaudRate = 9600;
            device.Parity = Parity.None;
            device.DataBits = 8;
            device.StopBits = StopBits.One;

            try
            {
                device.Open();
                isDeviceOpening = true;
                Console.WriteLine("串口已打开！");
                return true;
            }
            catch (Exception e)
            {
                Console.WriteLine("串口打开失败！");
                Console.WriteLine(e.Message);
                Console.Write(e.StackTrace);
                return false;
            }
        }

        public static void updateInfo()
        {
            while (true)
            {
                updateSpeed();
                updatePosition();
                // 1秒中更新一次；
                Thread.Sleep(2 * 1000);
            }
        }

        private static bool isDeviceOpening;
        public static void updateSpeed()
        {
            if (isDeviceOpening)
            {
                byte[] data = new byte[6];
                data[0] = 0x55;
                data[1] = 0x09;
                getSpeed(data);
                data[5] = getCheckCode(data);
                write(data);

                string str = bytes2HexString(data);
                Console.WriteLine("==========================");
                Console.WriteLine("更新速度:{0}", str);
            }
        }

        public static void updatePosition()
        {
            if (isMoving)
            {
                byte[] data = new byte[6];
                data[0] = 0x55;
                data[1] = 0x1a;
                data[2] = axis;
                getPos(data);
                data[5] = getCheckCode(data);
                write(data);

                string str = bytes2HexString(data);
                Console.WriteLine("==========================");
                Console.WriteLine("更新位置:{0}", str);
            }
        }
        
        static bool isError;
        private static void receivedata()
        {
            int size = 6;
            byte[] rec = new byte[size];
            int currCount = 0;
            while (true)
            {
                byte[] buff = new byte[size];
                int count = device.Read(buff, 0, size);
                int needCound = size - currCount;
                if (count > needCound)
                {
                    Array.Copy(buff, 0, rec, currCount, needCound);
                    //
                    response(rec);
                    rec = new byte[size];
                    currCount = 0;
                    Array.Copy(buff, needCound, rec, currCount, count - needCound);
                } else if (count < needCound)
                {
                    Array.Copy(buff, 0, rec, currCount, count);
                    currCount += count;
                } else
                {
                    Array.Copy(buff, 0, rec, currCount, needCound);
                    response(rec);
                    rec = new byte[size];
                    currCount = 0;
                }
                // 如果第一個byte不是0xAA，則數據錯誤
                // 需要丟棄AA之前的字符
                isError = true;
            }
        }

        private static void response(byte[] datas)
        {
            string str = bytes2HexString(datas);
            Console.WriteLine("==========================");
            Console.WriteLine("接收线程:{0}", str);
            //
            if (datas[0] == 0xAA)
            {
                datas[0] = 0x55;
                // 下发命令；数据校验
                if (getCheckCode(datas) != datas[5])
                {
                    setCheckCode(datas);
                    write(datas);
                    return;
                }
                // 执行命令
                if(datas[1] == 0x01)
                {
                    Console.WriteLine("设置零点");
                    curPos1 = 0;
                    curPos2 = 0;
                    curPos3 = 0;
                    curPos4 = 0;
                    curPos5 = 0;
                    curPos6 = 0;
                } 
                else if (datas[1] == 0x02)
                {
                    Console.WriteLine("设置速度");
                    setSpeed(datas);
                } else if (datas[1] == 0x03)
                {
                    Console.WriteLine("设置最小限位");
                    setMinPos(datas);
                } else if (datas[1] == 0x04)
                {
                    Console.WriteLine("设置最大限位");
                    setMaxPos(datas);
                } else if (datas[1] == 0x05)
                {
                    Console.WriteLine("开始点动");
                    startMove(datas);
                }
                else if (datas[1] == 0x06)
                {
                    Console.WriteLine("停止点动");
                    stopMove(datas);
                }
                else if (datas[1] == 0x07)
                {
                    Console.WriteLine("轨迹运行");
                }
                else if (datas[1] == 0x08)
                {
                    Console.WriteLine("切换模式");
                }
                //相应命令
                setCheckCode(datas);
                write(datas);
            } else if (datas[0] == 0x55)
            {
                // 上传命令
                //if (datas[1] == 0x09)
                //{
                //    Console.WriteLine("获取速度");
                //    getSpeed(datas);
                //}
                //else if (datas[1] == 0x0A)
                //{
                //    Console.WriteLine("获取位置");
                //    getPos(datas);
                //}
                //datas[5] = getCheckCode(datas);
                //write(datas);
            }
        }

        private static void write(byte[] buff)
        {
            device.Write(buff, 0, buff.Length);
        }

        private static String bytes2HexString(byte[] buff)
        {
            StringBuilder builder = new StringBuilder();
            if (buff == null || buff.Length <= 0)
                return "";

            for (int i = 0; i < buff.Length; i++)
            {
                builder.Append(buff[i].ToString("X2"));
            }

            return builder.ToString();
        }

        private static void setCheckCode(byte[] data)
        {
            byte checkCode = getCheckCode(data);
            if (checkCode == data[5]){
                data[3] = 0x00;
                data[4] = 0x00;
            } else
            {
                data[3] = 0x00;
                data[4] = 0x01;
            }
            data[5] = getCheckCode(data);
        }

        private static byte getCheckCode(byte[] data)
        {
            return (byte)((data[1] + data[2] + data[3] + data[4]) & 0xFF);
        }

        static void setSpeed(byte[] data)
        {
            // 传递速度单位为0.1转，所以需要乘以10；
            int rate = data[4];
            curSpeed = (Int16)(maxSpeed * rate * 0.01 * 10);
            Console.WriteLine(String.Format("当前速度{0}% : {1}", rate, curSpeed));
        }

        static void getSpeed(byte[] data)
        {
            data[3] = (byte)((curSpeed >> 8) & 0xFF);
            data[4] = (byte)(curSpeed & 0xFF);
        }

        static void setMinPos(byte[] data)
        {
            Int16 pos = (Int16)(((data[3] & 0xFF) << 8) | (data[4] & 0xFF));
            switch (data[2])
            {
                case 1:
                    minPos1 = pos;
                    break;
                case 2:
                    minPos2 = pos;
                    break;
                case 3:
                    minPos3 = pos;
                    break;
                case 4:
                    minPos4 = pos;
                    break;
                case 5:
                    minPos5 = pos;
                    break;
                case 6:
                    minPos6 = pos;
                    break;
            }
        }

        static void setMaxPos(byte[] data)
        {
            Int16 pos = (Int16)(((data[3] & 0xFF) << 8) | (data[4] & 0xFF));
            switch (data[2])
            {
                case 1:
                    maxPos1 = pos;
                    break;
                case 2:
                    maxPos2 = pos;
                    break;
                case 3:
                    maxPos3 = pos;
                    break;
                case 4:
                    maxPos4 = pos;
                    break;
                case 5:
                    maxPos5 = pos;
                    break;
                case 6:
                    maxPos6 = pos;
                    break;
            }
        }

        static void getPos(byte[] data)
        {
            // 模拟已经运动过了；
            if (isMoving)
                move(data[2], reverseDirection ? -5 : 5);
            //
            int pos = 0;
            switch (data[2])
            {
                default:
                case 1:
                    pos = curPos1;
                    break;
                case 2:
                    pos = curPos2;
                    break;
                case 3:
                    pos = curPos3;
                    break;
                case 4:
                    pos = curPos4;
                    break;
                case 5:
                    pos = curPos5;
                    break;
                case 6:
                    pos = curPos6;
                    break;
            }

            data[3] = (byte)((pos >> 8) & 0xFF);
            data[4] = (byte)(pos & 0xFF);
        }

        static void startMove(byte[] data)
        {
            isMoving = true;
            axis = data[2];
            reverseDirection = data[4] == 0x02;
        }

        static void stopMove(byte[] data)
        {
            isMoving = false;
        }
        
        static bool isMoving;
        static bool reverseDirection;
        static byte axis;

        static void move(int axis, int offset)
        {
            switch (axis)
            {
                case 1:
                    curPos1 += (Int16)offset;
                    if (curPos1 > maxPos1)
                        curPos1 = maxPos1;
                    if (curPos1 < minPos1)
                        curPos1 = minPos1;
                    break;
                case 2:
                    curPos1 += (Int16)offset;
                    if (curPos2 > maxPos2)
                        curPos2 = maxPos2;
                    if (curPos2 < minPos2)
                        curPos2 = minPos2;
                    break;
                case 3:
                    curPos3 += (Int16)offset;
                    if (curPos3 > maxPos3)
                        curPos3 = maxPos3;
                    if (curPos3 < minPos3)
                        curPos3 = minPos3;
                    break;
                case 4:
                    curPos4 += (Int16)offset;
                    if (curPos4 > maxPos4)
                        curPos4 = maxPos4;
                    if (curPos4 < minPos4)
                        curPos4 = minPos4;
                    break;
                case 5:
                    curPos5 += (Int16)offset;
                    if (curPos5 > maxPos5)
                        curPos5 = maxPos5;
                    if (curPos5 < minPos5)
                        curPos5 = minPos5;
                    break;
                case 6:
                    curPos6 += (Int16)offset;
                    if (curPos6 > maxPos6)
                        curPos6 = maxPos6;
                    if (curPos6 < minPos6)
                        curPos6 = minPos6;
                    break;
            }
        }

    }
}
