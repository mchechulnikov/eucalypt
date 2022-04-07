using System;
using System.Threading;

namespace ConsoleApplication
{
    public class Program
    {
        public static void Main(string[] args)
        {
            while(true) {
                Console.WriteLine("Running...");
                Thread.Sleep(3000);
            }
        }
    }
}