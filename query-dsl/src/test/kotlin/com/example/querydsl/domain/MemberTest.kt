package com.example.querydsl.domain


import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.JPAExpressions.*
import com.querydsl.jpa.impl.JPAQueryFactory
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import com.example.querydsl.domain.QMember.member as qMember
import com.example.querydsl.domain.QTeam.team as qTeam

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Transactional
internal class MemberTest(
        private val em: EntityManager
) {

    val query = JPAQueryFactory(em)

    @BeforeEach
    internal fun setUp() {
        val teamA = Team("teamA")
        val teamB = Team("teamB")

        em.persist(teamA)
        em.persist(teamB)

        val member1 = Member(username = "member1", age = 10, team = teamA)
        val member2 = Member(username = "member2", age = 20, team = teamA)
        val member3 = Member(username = "member3", age = 30, team = teamB)
        val member4 = Member(username = "member4", age = 40, team = teamB)

        em.persist(member1)
        em.persist(member2)
        em.persist(member3)
        em.persist(member4)

        em.flush()
        em.clear()
    }

    @Test
    internal fun `member save test`() {
        val members = em.createQuery("select m from Member m", Member::class.java).resultList
        for (member in members) {
            println("member -->> : $member")
        }
    }

    @Test
    internal fun `hibernate query`() {
        //when
        val member = em.createQuery("SELECT m from Member m where m.username = :username", Member::class.java)
                .setParameter("username", "member1")
                .singleResult

        //then
        then(member.username).isEqualTo("member1")
    }

    @Test
    internal fun `query dsl`() {
        val username = "member1"

        val member = query
                .select(qMember)
                .from(qMember)
                .where(qMember.username.eq(username))
                .fetchOne()!!

        then(member.username).isEqualTo(username)
    }

    @Test
    internal fun `query dsl search`() {
        //@formatter:off
        val member = query
                .selectFrom(qMember)
                .where(
                        qMember.username.eq("member1")
                        .and(qMember.age.eq(10))
                )
                .fetchOne()!!
        //@formatter:on

        then(member.username).isEqualTo("member1")
        then(member.age).isEqualTo(10)
    }

    @Test
    internal fun `query dsl and 생략 가능`() {
        val member = query
                .selectFrom(qMember)
                .where(
                        qMember.username.eq("member1"),
                        qMember.age.eq(10)
                )
                .fetchOne()!!

        then(member.username).isEqualTo("member1")
        then(member.age).isEqualTo(10)
    }

    @Test
    internal fun `query dsl fetch type`() {

        // 단건 조회
        val member = query
                .selectFrom(qMember)
                .where(qMember.username.eq("member1"))
                .fetchOne()

        // list 조회
        val members = query
                .selectFrom(qMember)
                .fetch()

        // 처음 한건 조회
        val firstMember = query
                .selectFrom(qMember)
                .fetchFirst()

        // 페이징 사용
        val pagingMembers = query
                .selectFrom(qMember)
                .fetchResults()

        // count 쿼리
        val count = query
                .selectFrom(qMember)
                .fetchCount()

    }

    @Test
    internal fun `query dsl sort`() {
        val members = query
                .selectFrom(qMember)
                .orderBy(qMember.age.desc(), qMember.username.asc().nullsLast())
                .fetch()
    }

    @Test
    internal fun `query dsl paging fetch 조회 건수 제한`() {
        val members = query
                .selectFrom(qMember)
                .orderBy(qMember.username.desc())
                .offset(1)
                .limit(2)
                .fetch()
    }

    @Test
    internal fun `query dsl paging fetch results 전체 조회 수가 필요`() {
        val paging = query
                .selectFrom(qMember)
                .orderBy(qMember.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults()


        then(paging.total).isEqualTo(4)
        then(paging.limit).isEqualTo(2)
        then(paging.offset).isEqualTo(1)
        then(paging.results.size).isEqualTo(2)
    }

    @Test
    internal fun `query dsl aggregation set`() {

        val result = query
                .select(
                        qMember.count(),
                        qMember.age.sum(),
                        qMember.age.avg(),
                        qMember.age.max(),
                        qMember.age.min()
                )
                .from(qMember)
                .fetch()


        val tuple = result[0]

        then(tuple.get(qMember.count())).isEqualTo(4)
        then(tuple.get(qMember.age.sum())).isEqualTo(100)
        then(tuple.get(qMember.age.avg())).isEqualByComparingTo(25.0)
        then(tuple.get(qMember.age.max())).isEqualTo(40)
        then(tuple.get(qMember.age.min())).isEqualTo(10)
    }

    @Test
    internal fun `query dsl group by`() {

        val result = query
                .select(qTeam.name, qMember.age.avg())
                .from(qMember)
                .join(qMember.team, qTeam)
                .groupBy(qTeam.name)
                .fetch()

        val teamA = result[0]
        val teamB = result[1]

        then(teamA.get(qTeam.name)).isEqualTo("teamA")
        then(teamA.get(qMember.age.avg())).isEqualTo(15.0)

        then(teamB.get(qTeam.name)).isEqualTo("teamB")
        then(teamB.get(qMember.age.avg())).isEqualTo(35.0)
    }

    @Test
    internal fun `query dsl join`() {

        val members = query
                .selectFrom(qMember)
                .join(qMember.team, qTeam)
                .where(qTeam.name.eq("teamA"))
                .fetch()

        then(members).anySatisfy {
            then(it.username).isIn("member1", "member2")
            then(it.team!!.name).isEqualTo("teamA")
        }
    }

    @Test
    internal fun `query dsl seta join`() {
        val members = query
                .select(qMember)
                .from(qMember, qTeam)
                .where(qMember.username.eq(qTeam.name))
                .fetch()
    }

    @Test
    internal fun `query dsl join on`() {
        val members = query
                .select(qMember)
                .from(qMember)
                .leftJoin(qMember.team, qTeam).on(qTeam.name.eq("teamA"))
                .fetch()

        then(members).anySatisfy {
            then(it.team!!.name).isEqualTo("teamA")
        }
    }

    @Test
    internal fun `query dsl ro relation`() {
        val result = query
                .select(qMember, qTeam)
                .from(qMember)
                .innerJoin(qTeam).on(qMember.username.eq(qTeam.name))
                .fetch()


        for (tuple in result) {
            println("tuple : ${tuple}")
        }
    }

    @Test
    internal fun `query dsl fetch join`() {
        val team = query
                .selectFrom(qTeam)
                .join(qTeam.members, qMember).fetchJoin()
                .where(qTeam.name.eq("teamA"))
                .fetchOne()!!
    }

    @Test
    internal fun `query dsl sub query 나이가 가장 큰 값`() {
        val qMemberSub = QMember("memberSub")
        val member = query
                .selectFrom(qMember)
                .where(qMember.age.eq(
                        select(qMemberSub.age.max())
                                .from(qMemberSub)
                ))
                .fetchOne()!!

        then(member.age).isEqualTo(40)
    }

    @Test
    internal fun `query dsl sub query 평균 보다 큰 나이`() {
        val qMemberSub = QMember("memberSub")
        val members = query
                .selectFrom(qMember)
                .where(qMember.age.goe(
                        select(qMemberSub.age.avg())
                                .from(qMemberSub)
                ))
                .fetch()!!

        then(members).anySatisfy {
            then(it.age).isIn(30, 40)
        }
    }
}