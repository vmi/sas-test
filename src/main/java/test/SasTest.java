/*
 * Copyright (c) 2018 Komatsu Ltd. All rights reserved.
 */
package test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

@SuppressWarnings("javadoc")
public class SasTest {

    private static void setupProxy(List<String> argList) {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("proxySet", "true");
        if (!argList.isEmpty()) {
            System.setProperty("proxyHost", argList.remove(0));
        }
        if (!argList.isEmpty()) {
            System.setProperty("proxyPort", argList.remove(0));
        }
        if (!argList.isEmpty()) {
            String pxid = argList.remove(0);
            String pxpw = argList.remove(0);
            Authenticator.setDefault(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(pxid, pxpw.toCharArray());
                }
            });
        }
    }

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            System.exit(1);
        }
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        String id = argList.remove(0);
        String pw = argList.remove(0);
        String gp = argList.remove(0);
        if (!argList.isEmpty()) {
            setupProxy(argList);
        }
        Instant now = Instant.now();
        String conn = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", id, pw);
        CloudStorageAccount account = CloudStorageAccount.parse(conn);
        CloudBlobClient client = account.createCloudBlobClient();
        CloudBlobContainer container = client.getContainerReference("test");
        container.createIfNotExists();
        StorageUri uri = container.getStorageUri();
        System.out.println("uri=" + uri);
        if (gp.equals("-")) {
            gp = null;
        } else {
            SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
            policy.setPermissions(EnumSet.allOf(SharedAccessBlobPermissions.class));
            BlobContainerPermissions permissions = new BlobContainerPermissions();
            permissions.getSharedAccessPolicies().put(gp, policy);
            container.uploadPermissions(permissions);
        }
        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setSharedAccessExpiryTime(Date.from(now.plus(1, ChronoUnit.HOURS)));
        policy.setPermissions(EnumSet.allOf(SharedAccessBlobPermissions.class));
        String sas = container.generateSharedAccessSignature(policy, gp);
        System.out.println("sas=" + sas);
        StorageCredentials credentials = new StorageCredentialsSharedAccessSignature(sas);
        CloudBlobContainer sasContainer = new CloudBlobContainer(uri, credentials);
        CloudBlockBlob blob = sasContainer.getBlockBlobReference("test.txt");
        blob.uploadText("Now=" + now);
        CloudBlockBlob blob2 = sasContainer.getBlockBlobReference("test.txt");
        System.out.println("Uploaded: test.txt");
        System.out.println("Downloaded: test.txt");
        blob2.download(System.out);
        System.out.println();
    }
}
