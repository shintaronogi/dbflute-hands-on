package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.unit.UnitContainerTestCase;

// TODO shiny javadoc by jflute (2025/04/16)
public class HandsOn03Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    /**
     * 会員名称がSで始まる1968年1月1日以前に生まれた会員を検索
     * 会員ステータスも取得する
     * 生年月日の昇順で並べる
     * 会員が1968/01/01以前であることをアサート
     */
    public void test_1() {
        // ## Arrange ##
        LocalDate targetDate = LocalDate.of(1968, 1, 1);

        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.query().setMemberName_LikeSearch("S", op -> op.likePrefix());
            cb.query().setBirthdate_LessEqual(targetDate);
            cb.query().addOrderBy_Birthdate_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        memberList.forEach(member -> {
            // TODO shiny ここでは、カラム名もBIRTHDATEだし、変数名も単に birthdate でもいいかなと by jflute (2025/04/16)
            // 変数のスコープの広さ次第で変数名をどれだけ修飾するか決まる。
            // but ここではカラム名を省略するわけではなく、テーブル名 prefix を外すだけ
            // 名前は識別するためのもの、識別する人のことを想像して判断する
            LocalDate memberBirthdate = member.getBirthdate();
            log(member.getMemberName(), memberBirthdate, member.getMemberStatus());
            assertTrue(memberBirthdate.isEqual(targetDate) || memberBirthdate.isBefore(targetDate));
            // 別のやり方: これが正解というわけではないが参考として
            //assertFalse(memberBirthdate.isAfter(targetDate));
        });
    }

    /**
     * 会員ステータスと会員セキュリティ情報も取得して会員を検索
     * 若い順で並べる。生年月日がない人は会員IDの昇順で並ぶようにする
     * 会員ステータスと会員セキュリティ情報が存在することをアサート
     */
    public void test_2() {
        // ## Arrange ##
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.setupSelect_MemberSecurityAsOne();
            cb.query().addOrderBy_Birthdate_Desc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);
        memberList.forEach(member -> {
            // [1on1でのふぉろー]
            // MemberStatusが100%存在する理由は？ => (完璧な回答)
            // (NotNullのFKカラムで参照しているから、探しに行けば絶対にする: 物理的に保証されている)
            assertTrue(member.getMemberStatus().isPresent());
            
            // [1on1でのふぉろー]
            // MemberSecurityが100%存在する理由は？ => (いっぱい考えた)
            // (カーディナリティのセカンドレベル、ERDだと黒丸、テーブルコメントにも書いてある)
            assertTrue(member.getMemberSecurityAsOne().isPresent());

            // TODO shiny [読み物課題] これ最重要 by jflute (2025/04/16)
            // 会員から会員ステータスは、NotNullのFKカラムで参照しているので、探しにいけば必ず存在する
            // 会員から会員セキュリティは、FKの方向と探しにいく方向が逆なので同じ理論にはなりませんが、
            // ERDのリレーションシップ線に注目。会員退会情報と比べると一目瞭然、黒丸がついていないので必ず存在する1
            //   会員から会員セキュリティ => 1:必ず1 (1:1)
            //   会員から会員退会情報    => 1:いないかもしれない1 (1:0..1)
            // ただ、物理的な制約はありません。業務的というのは、そういうルールにしているいうことだけなんですね。
            // 細かいですが、これがデータベースプログラミングにおいて、とても重要なんですよね。
            // ぜひ、カージナリティに着目してみてください。
            
            // [1on1でのふぉろー]
            // FK制約を物理的に貼るか貼らないか話、jfluteの意見を説明。
        });
    }
}
