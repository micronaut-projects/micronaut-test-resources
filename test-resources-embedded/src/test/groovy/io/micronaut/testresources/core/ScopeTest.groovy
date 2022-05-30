package io.micronaut.testresources.core

import spock.lang.Specification

class ScopeTest extends Specification {
    def "test scopes"() {
        def root = Scope.ROOT
        def root_a = Scope.of("a")
        def root_b = Scope.of("b")
        def root_a_a = root_a.child("a")
        def root_a_b = root_a.child("b")

        expect:
        root.includes(root_a)
        root.includes(root_b)
        root_a.includes(root_a_a)
        root_a.includes(root_a_b)
        !root_a.includes(root)
        !root_b.includes(root)
        !root_a_a.includes(root_a)
        !root_a_b.includes(root_a_a)

        and:
        root.toString() == ''
        root_a.toString() == 'a'
        root_a_a.toString() == 'a.a'
        root_a_b.toString() == 'a.b'

    }

    def "parses scopes"() {
        expect:
        Scope.of("a.b.c") == Scope.ROOT.child("a").child("b").child("c")

    }
}
