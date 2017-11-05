package com.dosar.sentiment

trait Vocab extends (String => Boolean){
  def lang: Option[String]
}
