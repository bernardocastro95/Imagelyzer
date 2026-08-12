package com.example.imagelyzer

data class AnalysisResult(
    val location: String, //Guess of where photo was taken / showed location
    val history: String, //Brief history
    val curiosities: List<String> //List of place's curiosities
)