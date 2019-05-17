### Missing due to time limit
 * Style is off, as I could not grok Vert.x yet. (My js code usually goes with async/await =p or at least Promise chaining)
 * Tests are dependent on each other
   * I did not have time to debug the issue I had with the `@Order` in junit 5.4
   * I did not want to go into multiple layers of callbacks, see style above
 * Did not have time to cover every feature with tests, like error scenarios
 * `MainVerticle` could be broken up even more

As a sum up:
 * Most time were spent on finding the right syntax for Vert.x
 * I have missed the time limit -- I have spent around 4 hours on prototyping the fixes, then one more to try to clean it up
 * From the optional issue, I have only looked at the URL validation, as it had to be handled anyway.
