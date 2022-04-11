using System;
using System.Net.Http;

var client = new HttpClient();
var content = await client.GetStringAsync("http://www.example.com/");

Console.WriteLine(content);