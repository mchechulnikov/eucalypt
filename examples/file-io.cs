using System;
using System.IO;

namespace ConsoleApplication
{
    public class Program
    {
        public static void Main(string[] args)
        {
            string[] lines =
            {
                "First line", "Second line", "Third line" 
            };

            var filePath = "test.txt";

            WriteToFile(filePath, lines);
            Console.WriteLine($"Lines has been written to file {filePath}");

            ReadFromFile(filePath);
            Console.WriteLine($"Lines has been read from file {filePath}");
        }

        public static void WriteToFile(string filePath, string[] lines)
        {
            File.WriteAllLinesAsync(filePath, lines).Wait();
        }

        public static void ReadFromFile(string filePath)
        {
            var text = File.ReadAllText(filePath);
            System.Console.WriteLine(text);
        }
    }
}