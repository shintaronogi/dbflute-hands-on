package org.docksidestage.handson.exercise;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.exception.NonSpecifiedColumnAccessException;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.MemberSecurityBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.*;
import org.docksidestage.handson.unit.UnitContainerTestCase;

// TODO done shiny javadoc by jflute (2025/04/16)

/**
 * @author shiny
 */
public class HandsOn03Test extends UnitContainerTestCase {

    @Resource
    private MemberBhv memberBhv;

    @Resource
    private MemberSecurityBhv memberSecurityBhv;

    @Resource
    private PurchaseBhv purchaseBhv;

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
            // TODO done shiny ここでは、カラム名もBIRTHDATEだし、変数名も単に birthdate でもいいかなと by jflute (2025/04/16)
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

            log(member);

            // [1on1でのふぉろー]
            // MemberSecurityが100%存在する理由は？ => (いっぱい考えた)
            // (カーディナリティのセカンドレベル、ERDだと黒丸、テーブルコメントにも書いてある)
            assertTrue(member.getMemberSecurityAsOne().isPresent());

            // TODO done shiny [読み物課題] これ最重要 by jflute (2025/04/16)
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

    /**
     * 会員セキュリティ情報のリマインダ質問で2という文字が含まれている会員を検索
     * 会員セキュリティ情報のデータ自体は要らない
     * (Actでの検索は本番でも実行されることを想定し、テスト都合でパフォーマンス劣化させないこと)
     * リマインダ質問に2が含まれていることをアサート
     * アサートするために別途検索処理を入れても誰も文句は言わない
     */
    public void test_3() {
        // ## Arrange ##
        String target = "2";
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberSecurityAsOne().setReminderQuestion_LikeSearch(target, op -> op.likeContain());
        });

        // ## Assert ##
        assertHasAnyElement(memberList);

        // 色々なやり方がありそう。脳の記憶をリフレッシュする為に、久々にStream APIを使ってみる。
        // List<Integer> memberIds = memberList.stream().map(member -> member.getMemberId()).collect(Collectors.toList());

        // Extractという便利なメソッドがあることに気づいた！
        Set<Integer> memberIds = memberList.extractColumnSet(member -> member.getMemberId());
        // これもできそう：memberBhv.extractMemberIdList(memberList);
        ListResultBean<MemberSecurity> memberSecurities = memberSecurityBhv.selectList(cb -> {
            // done jflute "IN" Operatorについて聞きたいです！
            // [回答] orScopeQuery()で同じカラムの等値条件と変わらないけど...
            // MySQLに対してわかりやすく示すという意味では、INの方が良いことがあるかも？
            // (少なくとも、そういう姿勢で実装の判断をしていった方が良い)
            //
            // ちなみに、in, in句、SQLの条件のinとか会話で聞き直すこと一杯。だからInScope。
            //
            // ローマ字のカラム名の歴史、時々シンプルな言葉でローマ字にするのは悪くないと思う。
            cb.query().setMemberId_InScope(memberIds);
        });
        memberList.forEach(member -> {
            Integer memberId = member.getMemberId();
            Optional<String> reminderQuestion = memberSecurities.stream()
                    .filter(security -> security.getMemberId().equals(memberId))
                    .findFirst()
                    .map(security -> security.getReminderQuestion());
            // Stream APIで検索は直感的に分かりずらい
            log("Name: {}, ReminderQuestion: {}", member.getMemberName(), reminderQuestion.get());
            assertTrue(reminderQuestion.isPresent());
            assertTrue(reminderQuestion.get().contains(target));
            // TODO done shiny ログ出すなら、assertよりも前の方が落ちた時に見れる (かつ、optionalのまま出してOK) by jflute (2025/04/23)
        });
    }

    /**
     * 会員ステータスの表示順カラムで会員を並べて検索
     * 会員ステータスの "表示順" カラムの昇順で並べる
     * 会員ステータスのデータ自体は要らない
     * その次には、会員の会員IDの降順で並べる
     * 会員ステータスのデータが取れていないことをアサート
     * 会員が会員ステータスごとに固まって並んでいることをアサート (順序は問わない)
     */
    public void test_4() {
        // ## Arrange ##
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.query().queryMemberStatus().addOrderBy_DisplayOrder_Asc();
            cb.query().addOrderBy_MemberId_Desc();
        });

        // ## Assert ##
        assertHasAnyElement(memberList);

        // ここも色々なやり方ありそう
        // やりやすいアサートとやりにくいやつがある
        // Lambdaだと並んでることをアサートみないなのはしにくい気がする

        // done shiny addOrderBy_DisplayOrder_Asc() を外して実行しても落ちない by jflute (2025/04/23)
        // [質問] Linkedの方が処理は重くなるのか？ (リンクしている分)
        // 確かに、1.4の時代に必要なければLinkedじゃないほうが良いとよく言われた。
        // ただ、現代のPCパワーだとほぼほぼ気にしなくてもいいくらいで、わりとアバウト。
        // LinkedHashMapに関しては、before/afterの参照を持っている。
        // 一方で、LinkedHashSetに関しては...あらりゃ？spliterator()だけORDEREDってあるけど？
        // TODO jflute LinkedHashSetの実装どうなってるの？ (2025/04/30)
        Set<String> memberStatusCodeSet = new HashSet<>();
        String previousStatusCode = "";
        for (Member member : memberList) {
            // 会員ステータスがとれていないことのアサート
            assertFalse(member.getMemberStatus().isPresent());

            String currentStatusCode = member.getMemberStatusCode();
            // 同じでなければ、セットに存在していないことをアサートして、追加。
            if (!currentStatusCode.equals(previousStatusCode)) {
                assertFalse(memberStatusCodeSet.contains(currentStatusCode));
            }
            memberStatusCodeSet.add(currentStatusCode);
            previousStatusCode = currentStatusCode;
        }
        // 分析的なアサートのやり方も紹介した
        //assertEquals(memberStatusCodeSet.size(), switchCount + 1);
    }

    // [1on1でのふぉろー] 基点テーブル
    // 結果セットにPKを付けるとしたら？

    // [1on1でのふぉろー] 要件を絶対間違えないプログラマー
    // 日本語の文章の構造を分析して解釈する習慣

    // TODO jflute 次回、サロゲートキーのお話 (2025/04/23)
    // https://dbflute.seasar.org/ja/manual/topic/dbdesign/surrogatekey.html
    // [1on1でのふぉろー] 話ししたー
    /**
     *生年月日が存在する会員の購入を検索
     * 会員名称と会員ステータス名称と商品名を取得する(ログ出力)
     * 購入日時の降順、購入価格の降順、商品IDの昇順、会員IDの昇順で並べる
     * OrderBy がたくさん追加されていることをログで目視確認すること
     * 購入に紐づく会員の生年月日が存在することをアサート
     */
    public void test_5() {
        // ## Arrange ##
        // ## Act ##
        ListResultBean<Purchase> purchases = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Product();
            // done shiny "生年月日が存在する" by jflute (2025/04/23)
            cb.query().queryMember().setBirthdate_IsNotNull();
            cb.query().addOrderBy_PurchaseDatetime_Desc();
            cb.query().addOrderBy_PurchasePrice_Desc();
            cb.query().addOrderBy_ProductId_Asc();
            cb.query().addOrderBy_MemberId_Asc();
        });

        // ## Assert ##
        assertHasAnyElement(purchases);
        purchases.forEach(purchase -> {
            Member member = purchase.getMember().orElseThrow();
            MemberStatus status = member.getMemberStatus().orElseThrow();
            // done shiny unusedになってる by jflute (2025/04/23)
            Product product = purchase.getProduct().orElseThrow();

            log("MemberName: {}, MemberStatus: {}, ProductName: {}", member.getMemberName(), status.getMemberStatusName(),
                    product.getProductId());

            log("birthdate: ", member.getBirthdate());
            assertNotNull(member.getBirthdate());
        });
    }

    /**
     * 2005年10月の1日から3日までに正式会員になった会員を検索
     * 画面からの検索条件で2005年10月1日と2005年10月3日がリクエストされたと想定して...
     * Arrange で String の "2005/10/01", "2005/10/03" を一度宣言してから日時クラスに変換し...
     * 自分で日付移動などはせず、DBFluteの機能を使って、そのままの日付(日時)を使って条件を設定
     * 会員ステータスも一緒に取得
     * ただし、会員ステータス名称だけ取得できればいい (説明や表示順カラムは不要)
     * 会員名称に "vi" を含む会員を検索
     * 会員名称と正式会員日時と会員ステータス名称をログに出力
     * 会員ステータスがコードと名称だけが取得されていることをアサート
     * 会員の正式会員日時が指定された条件の範囲内であることをアサート
     */
    public void test_6() {
        // ## Arrange ##
        // リクエストが文字列で送られてくると仮定
        String requestFrom = "2005/10/01";
        String requestTo = "2005/10/03";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime convertedFrom = LocalDate.parse(requestFrom, formatter).atStartOfDay();
        // TODO done shiny compareAsDate()的には、atTime(23, 59)はなくてOK。単純にLocalDateTimeに変換で by jflute (2025/04/30)
        // (一方で、59秒間の空白時間があるので、やるなら、23,59,59,999まで埋めちゃった方がいいかなと)
        LocalDateTime convertedTo = LocalDate.parse(requestTo, formatter).atStartOfDay();
        String targetName = "vi";

        adjustMember_FormalizedDatetime_FirstOnly(convertedFrom, targetName);
        // ## Act ##
        ListResultBean<Member> memberList = memberBhv.selectList(cb -> {
            cb.setupSelect_MemberStatus();
            cb.specify().specifyMemberStatus().columnMemberStatusName();
            cb.query().setMemberName_LikeSearch(targetName, op -> op.likeContain());
            cb.query().setFormalizedDatetime_FromTo(convertedFrom, convertedTo, op -> op.compareAsDate());
            // done dbflute 生成されたSQLみると、dfloc.FORMALIZED_DATETIME < '2005-10-04 00:00:00.000'になった！？
            // [1on1でのふぉろー] DateFromToのお話をした、日付は間違いやすい話も
            // Dateという概念自体が思いっきり人間の決め事ででているもの
        });
        // ## Assert ##
        assertHasAnyElement(memberList);
        memberList.forEach(member -> {
            String memberName = member.getMemberName();
            LocalDateTime formalizedDatetime = member.getFormalizedDatetime();
            MemberStatus status = member.getMemberStatus().orElseThrow();

            log("MemberName: {}, FormalizedDatetime: {}, StatusName: {}", memberName, formalizedDatetime, status);

            assertNotNull(status.getMemberStatusName());
            assertNotNull(status.getMemberStatusCode());

            // どういうExceptionがでるか試してみる
            // status.getDescription();
            assertException(NonSpecifiedColumnAccessException.class, () -> status.getDescription());
            assertException(NonSpecifiedColumnAccessException.class, () -> status.getDisplayOrder());

            assertTrue(formalizedDatetime.isEqual(convertedFrom) || formalizedDatetime.isAfter(convertedFrom));
            assertTrue(formalizedDatetime.isBefore(convertedTo.plusDays(1)));
        });
    }

    /**
     * 正式会員になってから一週間以内の購入を検索
     * 会員と会員ステータス、会員セキュリティ情報も一緒に取得
     * 商品と商品ステータス、商品カテゴリ、さらに上位の商品カテゴリも一緒に取得
     * 上位の商品カテゴリ名が取得できていることをアサート
     * 購入日時が正式会員になってから一週間以内であることをアサート
     */
    public void test_07() {
        // ## Arrange
        adjustPurchase_PurchaseDatetime_fromFormalizedDatetimeInWeek();
        // ## Act
        ListResultBean<Purchase> purchases = purchaseBhv.selectList(cb -> {
            cb.setupSelect_Member().withMemberStatus();
            cb.setupSelect_Member().withMemberSecurityAsOne();
            cb.setupSelect_Product().withProductStatus();
            cb.setupSelect_Product().withProductCategory().withProductCategorySelf();
            cb.columnQuery(colcb -> colcb.specify().columnPurchaseDatetime())
                    .greaterEqual(colcb -> colcb.specify().specifyMember().columnFormalizedDatetime());
            // 普通にplusでは正しく動いてなさそう
            // 綺麗な形ではないけど、一般的にDatetime型に+INTすると日時の加算として解釈してはくれそうな気はするが
            // とはいえ、基本的にSQLサーバーにはdateadd()関数あると思うので（Postgresとかではある）そっちを使ってみる
            cb.columnQuery(colcb -> colcb.specify().columnPurchaseDatetime())
                    .lessThan(colcb -> colcb.specify().specifyMember().columnFormalizedDatetime().convert(op -> op.addDay(8)));
        });
        // ## Assert
        assertHasAnyElement(purchases);
        purchases.forEach(purchase -> {
            Product product = purchase.getProduct().orElseThrow();
            // こういう時のorElseThrow()はちょっと冗長感あるので、Getと書きたくなる（まあどっちでもいいのだが）
            ProductCategory productCategory = product.getProductCategory().orElseThrow().getProductCategorySelf().orElseThrow();
            // なければ落ちるのだがassert
            assertNotNull(productCategory);
            LocalDateTime purchaseDatetime = purchase.getPurchaseDatetime();
            LocalDateTime formalizedDatetime = purchase.getMember().orElseThrow().getFormalizedDatetime();
            log("purchaseDatetime: {}, formalizedDatetime: {}", purchaseDatetime, formalizedDatetime);
            // adjustPurchase_PurchaseDatetime()の呼び出しで、取得できる件数が１増えた
            // 増えるか増えないかは元々の解釈で際どいところである。。笑
            // 理由は、1週間以内の定義で、その日を1週間のうちにみなすか、その日から1週間にするかの違い
            // 初期の自分の解釈では、その日から1週間以内 = つまり合計8日が対象になる
            // 追加されたpurchaseDatetimeがちょうど8日目の23:59なのでこの購入が増えることになる
            assertTrue(purchaseDatetime.isEqual(formalizedDatetime) || purchaseDatetime.isAfter(formalizedDatetime));
            assertTrue(purchaseDatetime.isBefore(formalizedDatetime.plusDays(8)));
        });
    }

    /**
     *
     */
    public void test_08() {

    }
}
