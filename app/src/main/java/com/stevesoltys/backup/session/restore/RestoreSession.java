package com.stevesoltys.backup.session.restore;

import android.app.backup.*;
import android.os.RemoteException;

import java.util.Set;

/**
 * @author Steve Soltys
 */
public class RestoreSession extends IRestoreObserver.Stub {

    private final IBackupManager backupManager;

    private final RestoreSessionObserver observer;

    private final Set<String> packages;

    private IRestoreSession restoreSession;

    public RestoreSession(IBackupManager backupManager, RestoreSessionObserver observer, Set<String> packages) {
        this.backupManager = backupManager;
        this.observer = observer;
        this.packages = packages;
    }

    public void start() throws RemoteException {

        if (restoreSession != null || packages.isEmpty()) {
            observer.restoreSessionCompleted(RestoreResult.FAILURE);
            return;
        }

        restoreSession = backupManager.beginRestoreSession(null, null);

        if (restoreSession == null) {
            stop(RestoreResult.FAILURE);
            return;
        }

        int result = restoreSession.getAvailableRestoreSets(this, null);

        if (result != BackupManager.SUCCESS) {
            stop(RestoreResult.FAILURE);
        }
    }

    public void stop(RestoreResult restoreResult) throws RemoteException {
        clearSession();
        observer.restoreSessionCompleted(restoreResult);
    }

    private void clearSession() throws RemoteException {
        if (restoreSession != null) {
            restoreSession.endRestoreSession();
            restoreSession = null;
        }
    }

    @Override
    public void restoreSetsAvailable(RestoreSet[] restoreSets) throws RemoteException {
        if (restoreSets.length > 0) {
            RestoreSet restoreSet = restoreSets[0];
            String[] packageArray = packages.toArray(new String[packages.size()]);
            int result = restoreSession.restoreSome(restoreSet.token, this, null, packageArray);

            if (result != BackupManager.SUCCESS) {
                stop(RestoreResult.FAILURE);
            }
        }
    }

    @Override
    public void restoreStarting(int numPackages) throws RemoteException {
        observer.restoreSessionStarted(numPackages);
    }

    @Override
    public void onUpdate(int nowBeingRestored, String currentPackage) throws RemoteException {
        observer.restorePackageStarted(nowBeingRestored, currentPackage);
    }

    @Override
    public void restoreFinished(int result) throws RemoteException {
        if (result == BackupManager.SUCCESS) {
            stop(RestoreResult.SUCCESS);

        } else if (result == BackupManager.ERROR_BACKUP_CANCELLED) {
            stop(RestoreResult.CANCELLED);

        } else  {
            stop(RestoreResult.FAILURE);
        }
    }
}
