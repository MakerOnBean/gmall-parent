package cloud.makeronbean.gmall.product.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.product.prop.MinioProp;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/admin/product")
public class FileUploadController {

    @Autowired
    private MinioProp minioProp;

    @PostMapping("/fileUpload")
    public Result fileUpdate(MultipartFile file){
        String url = "";
        try {
            // 创建MinIOClient客户端对象
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(minioProp.getEndpointUrl())
                            .credentials(minioProp.getAccessKey(), minioProp.getSecreKey())
                            .build();

            // 判断指定的'桶'是否存在
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProp.getBucketName()).build());
            if (!found) {
                // 如果不存在指定的桶，则创建
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProp.getBucketName()).build());
            }

            // 文件名称
            String fileName = System.currentTimeMillis()+ UUID.randomUUID().toString();

            // Upload known sized input stream.
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioProp.getBucketName()).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            // 拼接url
            url = minioProp.endpointUrl + "/" + minioProp.bucketName + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(url);
    }

}
