package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;

import java.util.Optional;
import java.util.UUID;

/**
 * الفاحص الوحيد المعتمد لصلاحيات الكلان الداخلية. لا يُثق أبدًا بحالة
 * الـGUI أو الـclient - كل قرار يُبنى على حالة قاعدة البيانات/الكاش
 * الحالية فقط (قسم 89 من المواصفات: "لا تثق بالـGUI click وحده").
 */
public final class PermissionService {

    private final ClanService clanService;

    public PermissionService(ClanService clanService) {
        this.clanService = clanService;
    }

    public boolean hasPermission(UUID playerUuid, ClanPermission permission) {
        Optional<ClanMember> memberOpt = clanService.getMember(playerUuid);
        if (memberOpt.isEmpty()) {
            return false;
        }
        ClanMember member = memberOpt.get();

        Optional<Clan> clanOpt = clanService.getClan(member.getClanId());
        if (clanOpt.isPresent() && clanOpt.get().getOwnerUuid().equals(playerUuid)) {
            return true; // المالك يملك كل الصلاحيات دائمًا
        }

        Optional<ClanRankDefinition> rankOpt = clanService.getRank(member.getClanId(), member.getRankKey());
        return rankOpt.isPresent() && rankOpt.get().hasPermission(permission.name());
    }

    public boolean isOwner(UUID playerUuid) {
        Optional<ClanMember> memberOpt = clanService.getMember(playerUuid);
        if (memberOpt.isEmpty()) {
            return false;
        }
        Optional<Clan> clanOpt = clanService.getClan(memberOpt.get().getClanId());
        return clanOpt.isPresent() && clanOpt.get().getOwnerUuid().equals(playerUuid);
    }

    /**
     * يقارن أولوية رتبتين (لمنع ترقية/طرد عضو أعلى منك رتبة، مثال شائع
     * في أنظمة الكلانات المحترفة رغم عدم ذكره صراحة - يُعتبر حماية منطقية).
     */
    public boolean canManage(UUID actorUuid, UUID targetUuid) {
        if (isOwner(actorUuid)) {
            return true; // المالك يستطيع إدارة أي عضو آخر في كلانه
        }
        Optional<ClanMember> actorMember = clanService.getMember(actorUuid);
        Optional<ClanMember> targetMember = clanService.getMember(targetUuid);
        if (actorMember.isEmpty() || targetMember.isEmpty()) {
            return false;
        }
        if (!actorMember.get().getClanId().equals(targetMember.get().getClanId())) {
            return false;
        }
        Optional<ClanRankDefinition> actorRank = clanService.getRank(actorMember.get().getClanId(), actorMember.get().getRankKey());
        Optional<ClanRankDefinition> targetRank = clanService.getRank(targetMember.get().getClanId(), targetMember.get().getRankKey());
        if (actorRank.isEmpty() || targetRank.isEmpty()) {
            return false;
        }
        return actorRank.get().getPriority() > targetRank.get().getPriority();
    }
}
