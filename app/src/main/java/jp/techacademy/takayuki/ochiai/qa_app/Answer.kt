package jp.techacademy.takayuki.ochiai.qa_app

import java.io.Serializable

class Answer(val body: String, val name: String, val uid: String, val answerUid: String) : Serializable {
}