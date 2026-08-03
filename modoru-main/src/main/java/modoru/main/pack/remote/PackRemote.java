package modoru.main.pack.remote;

import modoru.main.MainConfiguration;
import modoru.main.pack.PackProcessor;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.FileUtil;
import su.hitori.api.util.IOUtil;
import su.hitori.api.util.LoggerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public final class PackRemote {

    private static final String INFO_FILE = "remote/local";
    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final MainConfiguration configuration;
    private final GithubResolver githubResolver;
    private final PackProcessor packProcessor;
    private final File moduleFolder;

    public PackRemote(MainConfiguration configuration, ExecutorService executorService, PackProcessor packProcessor, File moduleFolder) {
        this.configuration = configuration;
        this.githubResolver = new GithubResolver(executorService);
        this.packProcessor = packProcessor;
        this.moduleFolder = moduleFolder;

        String token = configuration.packRemote.token.get();
        if(!token.isEmpty()) githubResolver.token(token);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void performBlocking() {
        var config = configuration.packRemote;
        if(!config.enabled.get()) {
            LOGGER.info("Resolving pack from remote is disabled in config.");
            return;
        }

        boolean checkOnStart = config.checkOnStart.get();

        moduleFolder.mkdirs();
        File infoFile = new File(moduleFolder, INFO_FILE);
        if(!infoFile.exists() && !checkOnStart) {
            LOGGER.warning("Resolving pack from remote is enabled but checkOnStart is not and no currently info is present! Pack generation may fail.");
            return;
        }

        String localCommit;
        if(!infoFile.exists()) localCommit = "";
        else {
            try (FileInputStream fis = new FileInputStream(infoFile)) {
                localCommit = new String(IOUtil.readInputStream(fis), StandardCharsets.UTF_8);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        String latestCommit;
        try {
            latestCommit = githubResolver.latestCommitSHA(config.repo.get(), config.branch.get()).get();
        }
        catch (Throwable throwable) {
            LOGGER.warning("Unable to resolve latest commit for pack remote: " + LoggerUtil.exceptionToString(throwable));
            return;
        }

        if(!latestCommit.equals(localCommit)) LOGGER.info(String.format("HEAD is now at %s, updating...", latestCommit));
        else {
            // verify
            File packFolder = new File(moduleFolder, "remote/" + localCommit + "/");
            packProcessor.packFolder(packFolder);
            if(packFolder.exists()) {
                LOGGER.info("Local version is up to date.");
                return;
            }

            LOGGER.warning("Pack folder is missing, re-downloading...");
        }


        File contentsFolder = new File(moduleFolder, "remote/" + latestCommit + "/");
        contentsFolder.mkdirs();

        File outArchive = new File(contentsFolder, "archive.zip");
        try (FileOutputStream fos = new FileOutputStream(outArchive)) {
            githubResolver.downloadZipBall(config.repo.get(), latestCommit, fos);
            fos.flush();
        }
        catch (Throwable throwable) {
            LOGGER.warning("Unable to download repository contents: " + LoggerUtil.exceptionToString(throwable));
            return;
        }

        try {
            modoru.main.util.IOUtil.unzip(contentsFolder, outArchive);
        }
        catch (Throwable throwable) {
            LOGGER.warning("Unable to unzip repository contents: " + LoggerUtil.exceptionToString(throwable));
            return;
        }

        File[] files = contentsFolder.listFiles();
        if(files == null || files.length != 2) {
            LOGGER.severe("Unexpected I/O error occurred.");
            return;
        }

        File folder = files[0];
        if(!folder.isDirectory()) folder = files[1];
        if(!folder.isDirectory()) {
            LOGGER.severe("Problem while unarchiving archive: the only file that was in archive not a directory.");
            return;
        }

        try {
            modoru.main.util.IOUtil.moveFilesRecursively(folder, contentsFolder);
        }
        catch (Throwable throwable) {
            LOGGER.warning("Unable copy repository contents: " + LoggerUtil.exceptionToString(throwable));
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(infoFile)) {
            fos.write(latestCommit.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        }
        catch (Throwable throwable) {
            LOGGER.warning("Unable to write info file: " + LoggerUtil.exceptionToString(throwable));
            return;
        }

        if(config.deleteOldRevisions.get() && !localCommit.isEmpty()) {
            File oldContents = new File(moduleFolder, "remote/" + localCommit + "/");
            if(oldContents.exists()) FileUtil.deleteRecursively(oldContents);
        }

        packProcessor.packFolder(contentsFolder);

        LOGGER.info("Successfully updated local!");
    }

}
