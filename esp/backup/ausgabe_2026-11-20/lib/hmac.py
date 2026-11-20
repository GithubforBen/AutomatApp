import hashlib
import binascii


def _const_compare_digest(a, b):
    if isinstance(a, str) and isinstance(b, str):
        a = a.encode()
        b = b.encode()
    elif isinstance(a, (bytes, bytearray)) and isinstance(b, (bytes, bytearray)):
        a = bytes(a)
        b = bytes(b)
    else:
        raise TypeError("unsupported types")

    result = len(a) ^ len(b)
    for x, y in zip(a, b):
        result |= x ^ y
    return result == 0

def _guess_block_size(digestmod, h):
    name = ""

    if isinstance(digestmod, str):
        name = digestmod.lower()
    else:
        name = getattr(h, "name", "").lower()

    if "sha384" in name or "sha512" in name:
        return 128

    return 64


compare_digest = _const_compare_digest


def _hash_factory(digestmod):
    if digestmod is None:
        raise TypeError("Missing required argument 'digestmod'")

    if isinstance(digestmod, str):
        return lambda data=b"": getattr(hashlib, digestmod)(data)

    if callable(digestmod):
        return digestmod

    if hasattr(digestmod, "new"):
        return digestmod.new

    raise TypeError("unsupported digestmod")


class HMAC:
    def __init__(self, key, msg=None, digestmod=None):
        if not isinstance(key, (bytes, bytearray)):
            raise TypeError("key: expected bytes or bytearray")

        self._make_hash = _hash_factory(digestmod)

        test_hash = self._make_hash()
        self.digest_size = getattr(test_hash, "digest_size", None)
        self.block_size = getattr(test_hash, "block_size", None)

        if not self.block_size or self.block_size < 16:
            self.block_size = _guess_block_size(digestmod, test_hash)

        key = bytes(key)

        if len(key) > self.block_size:
            key = self._make_hash(key).digest()

        key += b"\x00" * (self.block_size - len(key))

        self._outer_pad = bytes((x ^ 0x5C) for x in key)
        self._inner_pad = bytes((x ^ 0x36) for x in key)

        # Store messages so digest() can be repeatable even without hash.copy().
        self._msgs = []

        self._inner = self._make_hash()
        self._inner.update(self._inner_pad)

        self._outer = self._make_hash()
        self._outer.update(self._outer_pad)

        self._has_copy = hasattr(self._inner, "copy") and hasattr(self._outer, "copy")

        if msg is not None:
            self.update(msg)

    @property
    def name(self):
        n = getattr(self._inner, "name", type(self._inner).__name__)
        return "hmac-" + n.lower().replace("_", "-")

    def update(self, msg):
        if not isinstance(msg, (bytes, bytearray)):
            raise TypeError("object supporting the buffer API required")

        msg = bytes(msg)
        self._msgs.append(msg)
        self._inner.update(msg)

    def copy(self):
        other = self.__class__.__new__(self.__class__)
        other._make_hash = self._make_hash
        other.digest_size = self.digest_size
        other.block_size = self.block_size
        other._outer_pad = self._outer_pad
        other._inner_pad = self._inner_pad
        other._msgs = self._msgs[:]
        other._has_copy = self._has_copy

        if self._has_copy:
            other._inner = self._inner.copy()
            other._outer = self._outer.copy()
        else:
            other._inner = self._make_hash()
            other._inner.update(self._inner_pad)
            for m in other._msgs:
                other._inner.update(m)

            other._outer = self._make_hash()
            other._outer.update(self._outer_pad)

        return other

    def _current(self):
        if self._has_copy:
            h = self._outer.copy()
            h.update(self._inner.copy().digest())
            return h

        inner = self._make_hash()
        inner.update(self._inner_pad)
        for m in self._msgs:
            inner.update(m)

        outer = self._make_hash()
        outer.update(self._outer_pad)
        outer.update(inner.digest())
        return outer

    def digest(self):
        return self._current().digest()

    def hexdigest(self):
        return binascii.hexlify(self.digest()).decode("ascii")
        


def new(key, msg=None, digestmod=None):
    return HMAC(key, msg, digestmod)


def digest(key, msg, digest):
    return HMAC(key, msg, digest).digest()
