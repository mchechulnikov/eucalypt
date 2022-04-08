using System;

namespace ConsoleApplication
{
    public class Program
    {
        public static void Main(string[] args)
        {
            int n = 42, first = 0, second = 1, third = 0;

            Console.Write("First {0} Fibonacci numbers {1} {2} ", n, first, second);

            for (int i = 3; i <= n; i++)
            {
                third = first + second;
                Console.Write("{0} ", third);
                first = second;
                second = third;
            }
        }
    }
}