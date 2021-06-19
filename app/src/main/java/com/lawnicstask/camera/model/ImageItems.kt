package com.lawnicstask.camera.model

data class ImageItems(var uniqueImageId : Long,
                      var id : String,
                      var imageName : String,
                      var imgType : String,
                      var imgUrl : String,
                      var date : String,
                      var userName : String,
                      var pages : Long,
                      var uploadedTime : String){

    constructor() : this(0L,"","","","","","",0,"")
}
