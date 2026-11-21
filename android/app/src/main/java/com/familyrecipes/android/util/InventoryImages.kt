package com.familyrecipes.android.util
import android.content.Context
import android.net.Uri
import android.graphics.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InventoryImages {
    suspend fun copy(context:Context,uri:Uri):File=withContext(Dispatchers.IO) {
        val resolver=context.contentResolver
        val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        resolver.openInputStream(uri).use{BitmapFactory.decodeStream(it,null,opts)}
        require(opts.outWidth>0&&opts.outHeight>0){"Invalid image"}
        opts.inSampleSize=1
        while(maxOf(opts.outWidth,opts.outHeight)/opts.inSampleSize>2000)opts.inSampleSize*=2
        opts.inJustDecodeBounds=false
        var bitmap=resolver.openInputStream(uri).use{BitmapFactory.decodeStream(it,null,opts)}?:error("Invalid image")
        val orientation=resolver.openInputStream(uri).use{if(it==null)1 else android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1)}
        if(orientation in 2..8){val matrix=Matrix().apply{when(orientation){2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f);5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f);7->{setRotate(-90f);postScale(-1f,1f)};8->setRotate(-90f)}};val rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true);if(rotated!==bitmap)bitmap.recycle();bitmap=rotated}
        val file=File.createTempFile("inventory-draft-",".jpg",context.filesDir)
        try{file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,88,it)}}finally{bitmap.recycle()}
        if(file.length()>8*1024*1024){file.delete();error("Image exceeds 8MB")};file
    }
}
