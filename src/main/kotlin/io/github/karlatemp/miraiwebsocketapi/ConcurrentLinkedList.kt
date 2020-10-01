/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/10/01 15:01:38
 *
 * mirai-websocket-api/ConcurrentLinkedList.kt
 */

package io.github.karlatemp.miraiwebsocketapi

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/*
 * Status: allocated, inserted, removed
 *
 *
 * Insert:
 * --------[Previous]-----------[Next]-----------
 *                   [Value]
 *
 * --------[Previous]-----------[Next]-----------
 *                 [Value(Locked)]
 *
 * --------[Locked P]-----------[Next]-----------
 *                 [Value(Locked)]
 *
 * --------[Locked P]-----------[L  N]-----------
 *                 [Value(Locked)]
 *
 * --------[Locked P]-----------[L  N]-----------
 *              |                  |
 *              |-[Value(Locked)] -|
 *
 * --------[Locked P]           [L  N]-----------
 *              |                  |
 *              |-[Value(Locked)] -|
 *
 * --------[Locked P]---[Value(Locked)]----[L  N]-----------
 *
 * --------[Previous]---[    Value    ]----[Next]-----------
 *
 *
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ConcurrentLinkedList<T> : Iterable<T> {
    open class LinkNode<T>() {
        constructor(value: T) : this() {
            this.value = value
        }

        enum class NodeStatus {
            ALLOCATED, INSERTED, REMOVED
        }

        protected var _status = NodeStatus.ALLOCATED

        lateinit var previous: LinkNode<T>
            protected set
        lateinit var next: LinkNode<T>
            protected set

        internal fun internalSetNext(next: LinkNode<T>) {
            this.next = next
        }

        internal fun internalSetStatus(status: NodeStatus) {
            this._status = status
        }

        internal fun internalSetPrevious(previous: LinkNode<T>) {
            this.previous = previous
        }

        private val lock = AtomicBoolean(false)

        @get:JvmName("get")
        @set:JvmName("set")
        var value by AtomicReference<T>()

        fun tryLock(): Boolean = lock.compareAndSet(false, true)
        fun unlock() {
            lock.set(false)
        }

        val status: NodeStatus get() = _status

        @Suppress("ControlFlowWithEmptyBody", "DuplicatedCode")
        open fun insertAfter(newNode: LinkNode<T>) {
            while (!newNode.tryLock());
            if (newNode._status == NodeStatus.INSERTED) {
                newNode.unlock()
                throw IllegalStateException("New node was inserted")
            }
            while (true) {
                if (tryLock()) {
                    if (!next.tryLock()) {
                        unlock()
                        continue
                    }
                    val n = next
                    newNode.previous = this
                    newNode.next = n
                    this.next = newNode
                    n.previous = newNode
                    newNode._status = NodeStatus.INSERTED
                    newNode.unlock()
                    n.unlock()
                    unlock()
                    break
                }
            }
        }

        @Suppress("ControlFlowWithEmptyBody", "DuplicatedCode")
        open fun insertBefore(newNode: LinkNode<T>) {
            while (!newNode.tryLock());
            if (newNode._status == NodeStatus.INSERTED) {
                newNode.unlock()
                throw IllegalStateException("New node was inserted")
            }
            while (true) {
                if (this.tryLock()) {
                    if (!previous.tryLock()) {
                        this.unlock()
                        continue
                    }
                    val pre = previous
                    newNode.previous = pre
                    newNode.next = this
                    this.previous = newNode
                    pre.next = newNode
                    newNode._status = NodeStatus.INSERTED
                    newNode.unlock()
                    pre.unlock()
                    this.unlock()
                    break
                }
            }
        }

        @Suppress("ControlFlowWithEmptyBody", "DuplicatedCode")
        open fun remove() {
            if (this._status == NodeStatus.ALLOCATED)
                throw IllegalStateException("Node not inserted")
            while (true) {
                if (this._status == NodeStatus.REMOVED) return
                if (tryLock()) {
                    if (!previous.tryLock()) {
                        this.unlock()
                        continue
                    }
                    if (!next.tryLock()) {
                        previous.unlock()
                        this.unlock()
                        continue
                    }
                    val p = previous
                    val n = next
                    _status = NodeStatus.REMOVED
                    p.next = n
                    n.previous = p
                    p.unlock()
                    n.unlock()
                    unlock()
                    break
                }
            }
        }

        open val isHead: Boolean
            get() = false

        open val isTail: Boolean
            get() = false
    }

    class Tail<T> : LinkNode<T>() {
        init {
            _status = NodeStatus.INSERTED
        }

        override val isTail: Boolean
            get() = true

        override fun insertAfter(newNode: LinkNode<T>) {
            insertBefore(newNode)
        }

        override fun remove() {
            throw IllegalStateException("Cannot remove tail node.")
        }

        internal fun init(head: Head<T>) {
            previous = head
        }
    }

    private abstract inner class NodeIterator<A>(
        private val reverse: Boolean
    ) : MutableIterator<A> {
        private var current: LinkNode<T> = if (reverse) tail else head
        private var ending: LinkNode<T> = if (reverse) head else tail
        private var nextNode: LinkNode<T>? = null

        override fun hasNext(): Boolean {
            if (nextNode != null) {
                return true
            }
            //println("C$current, E:$ending")
            if (current === ending) return false
            val nxt = if (reverse) {
                current.previous
            } else {
                current.next
            }
            if (nxt === ending) {
                current = ending
                return false
            }
            nextNode = nxt
            return true
        }

        open fun nextNode(): LinkNode<T> {
            if (nextNode != null) {
                return nextNode!!.also { current = it;nextNode = null }
            }
            if (hasNext()) {
                return nextNode!!.also { current = it;nextNode = null }
            }
            throw NoSuchElementException()
        }

        override fun remove() {
            current.remove()
        }
    }

    fun reverseNodeIterator(): Iterator<LinkNode<T>> {
        return object : NodeIterator<LinkNode<T>>(true), Iterator<LinkNode<T>> {
            override fun next(): LinkNode<T> = nextNode()
        }
    }

    fun nodeIterator(): Iterator<LinkNode<T>> {
        return object : NodeIterator<LinkNode<T>>(false), Iterator<LinkNode<T>> {
            override fun next(): LinkNode<T> = nextNode()
        }
    }

    fun reverseIterator(): Iterator<T> {
        return object : NodeIterator<T>(true), Iterator<T> {
            override fun next(): T = nextNode().value
        }
    }

    override fun iterator(): Iterator<T> {
        return object : NodeIterator<T>(false), Iterator<T> {
            override fun next(): T = nextNode().value
        }
    }

    class Head<T> : LinkNode<T>() {
        override val isHead: Boolean
            get() = true

        init {
            _status = NodeStatus.INSERTED
        }

        override fun insertBefore(newNode: LinkNode<T>) {
            insertAfter(newNode)
        }

        fun dropFirst(): LinkNode<T>? {
            while (true) {
                @Suppress("ControlFlowWithEmptyBody")
                while (!tryLock());
                val next = this.next
                if (!next.tryLock()) {
                    this.unlock()
                    continue
                }
                if (next.isTail) {
                    this.unlock()
                    next.unlock()
                    return null
                }
                val nextNext = next.next
                if (!nextNext.tryLock()) {
                    this.unlock()
                    next.unlock()
                    continue
                }
                this.next = nextNext
                next.internalSetStatus(NodeStatus.REMOVED)
                nextNext.internalSetPrevious(this)
                this.unlock()
                next.unlock()
                nextNext.unlock()
                return next
            }
        }

        override fun remove() {
            throw IllegalStateException("Cannot remove tail node.")
        }

        internal fun init(tail: Tail<T>) {
            next = tail
        }
    }

    val tail = Tail<T>()
    val head = Head<T>()

    init {
        tail.init(head)
        head.init(tail)
    }

    fun insertFirst(value: T): LinkNode<T> = LinkNode(value).also {
        head.insertAfter(it)
    }

    fun insertLast(value: T): LinkNode<T> = LinkNode(value).also {
        tail.insertBefore(it)
    }

    fun isEmpty(): Boolean = head.next === tail
    override fun toString(): String {
        return buildString {
            append('[')
            val iter = this@ConcurrentLinkedList.iterator()
            if (iter.hasNext()) {
                append(iter.next())
                iter.forEach {
                    append(", ").append(it)
                }
            }
            append(']')
        }
    }
}