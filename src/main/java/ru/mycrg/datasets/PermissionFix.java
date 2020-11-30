package ru.mycrg.datasets;

public class PermissionFix {

    private String oldId;
    private String newId;

    public PermissionFix(String oldId, String newId) {
        this.oldId = oldId;
        this.newId = newId;
    }

    public String getOldId() {
        return oldId;
    }

    public String getNewId() {
        return newId;
    }

    @Override
    public String toString() {
        return oldId + " ---> " + newId;
    }
}
