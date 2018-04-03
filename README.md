# Leased lock
The leased lock provides a lock service that can be used in an embedded and distributed manner. The idea is to provide locks that are somewhere between a mutex and a binary semaphore in terms of ownership semantics plus also provide leasing in order for us to not indefinitely hold on to shared resources. Locks come with fencing tokens which should be propagated across applications using the lock service. These fencing tokens allow for safe modifications of shared resources.

The interface provided is simple enough to provide support for more backend implementations based off of a database or a file system.

# Usage Manual
