using System;
using System.IO;

namespace ConsoleApplication
{
    public class Program
    {
        public static void Main(string[] args)
        {
            ExampleAsync();

            var text = File.ReadAllText(@"/app/test.txt");
            System.Console.WriteLine(text);

            Console.WriteLine("Hello World!");
        }

        public static void ExampleAsync()
        {
            string[] lines =
            {
                "First line", "Second line", "Third line" 
            };

            File.WriteAllLinesAsync("test.txt", lines).Wait();
        }
    }
}