-- Keep any legacy story objects private while the bucket is retired.
update storage.buckets set public = false where id = 'samplay-stories';
