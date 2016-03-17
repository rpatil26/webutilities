webutilities
============
Java library that helps you to **speed up and improve client side performance** of your JEE web application.

[![Build Status](https://travis-ci.org/rpatil26/webutilities.svg?branch=master)](https://travis-ci.org/rpatil26/webutilities) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.googlecode.webutilities/webutilities/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.googlecode.webutilities/webutilities/)
[![License](https://img.shields.io/badge/license-Apache_2-blue.svg?style=flat)](https://github.com/rpatil26/webutilities/blob/master/LICENSE)

### Introduction

Client side performance is important for better user experience. Optimizing and efficiently serving the static resources (JS, HTML, CSS, Images etc.) significantly increases client side performance. This Java Library provides web components to help you speed up Front-End of your J2EE application.

It is said that 80% of the end-user response time is spent on the front-end. To make the front-end efficient and friendly to the browser, various [Performance Practices](http://developer.yahoo.com/performance/rules.html) have been suggested. We can measure page performance using tools such as [YSlow](http://developer.yahoo.com/yslow/) or [Page Speed](http://code.google.com/speed/page-speed/). These tools validate page against the best practices and give the performance ratings/grade. WebUtilities provides inbuilt J2EE components to apply some of those best practices in your web application with minimal change to speed it up and get higher performance score. 

### Features

*   Serve multiple JS or CSS files in one request
*   Add Expires header for JS, CSS and Image files to be cached by browser
*   Minify JS, CSS files on the fly
*   Minify Inline CSS and JS code blocks
*   Add Character Encoding to your response
*   Server compressed contents (gzip/compress/deflate)
*   Cache responses to speed loading by avoiding reprocessing 

### Get started

*   Refer [Getting Started wiki] (https://github.com/rpatil26/webutilities/wiki/)
*   Configure your `web.xml` with [chain of filters] (https://github.com/rpatil26/webutilities/wiki/chaining) accordingly. 
   
For more examples and step by step Guide, Visit [Wiki](https://github.com/rpatil26/webutilities/wiki/). For any issues or have feature suggestions, report them [here] (https://github.com/rpatil26/webutilities/issues).

### Dependencies

WebUtilities uses Maven to manage the dependencies. Please refer the maven [Artifact Details] (https://maven-badges.herokuapp.com/maven-central/com.googlecode.webutilities/webutilities/) for the list of dependencies.

### Difference 
| Without Webutilities  | With Webutilities |
| ------------- | ------------- |
|![](http://rawgit.com/rpatil26/webutilities/gh-pages/images/before.png)|![](http://rawgit.com/rpatil26/webutilities/gh-pages/images/after.png)|



