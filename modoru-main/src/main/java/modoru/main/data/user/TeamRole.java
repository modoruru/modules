package modoru.main.data.user;

public enum TeamRole {

    MEDIA,

    JUNIOR_AGENT,
    AGENT,
    SENIOR_AGENT,

    SUPERVISOR,

    ADMINISTRATOR,

    TECHNICAL_ADMINISTRATOR;

    public boolean hasPermission(TeamRole teamRole) {
        return this.ordinal() >= teamRole.ordinal();
    }

}
