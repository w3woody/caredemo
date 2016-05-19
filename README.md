# caredemo
This is currently an in-progress project, and is currently incomplete.

## What is it?

When Apple released [CareKit](http://carekit.org), I thought it would be
interesting to put together a demonstration application which includes
a back-end service. The idea is that a doctor can enter a care plan on a
remote server, and have the CareKit-based application synchronize with 
that care plan on a local device. The eventual goal was to demonstrate
synchronizing of CareKit measurements with a back end in a secure fashion,
with a simple back end which uses access controls to limit access to
patient information.

The code currently contained here is not HIPPA compliant. Specifically
not logging has been wired in to track access to patient data. This code
should not be used in a medical setting, at least without a thorough
understanding of the security issues involved with handling patient data,
and without adding the necessary code for HIPPA compliance, including
finer grained security access, and security auditing.

However, the current code does demonstrate a few interesting features:

- A [Diffie-Hellman key exchange](https://en.wikipedia.org/wiki/Diffie–Hellman_key_exchange)
  which encrypts the data that is exchanged. Private confidential information
  is encrypted in such a way so that even using a tool like 
  [Charles](http://www.charlesproxy.com) to reveal the contents of the
  packets will reveal no confidential information. (This algorithm is
  even implemented within GWT, so confidential information on the front-end
  web site is preserved in the face of an SSL 
  ["man in the middle"](https://en.wikipedia.org/wiki/Man-in-the-middle_attack)
  attack. Note that a number of firewall/antivirus products will generate a
  certificate which permits those products to examine all data between
  a client and server--so simply using SSL is insufficient in securing data
  between a client and server.)
  
- An "Apple-TV" style login process, whereby the user logs in on the web, and
  adds his mobile device by entering an 8-character string displayed on 
  his phone. This greatly simplifies the process of adding a device to
  an account--which is important for elder patients who may not have the
  motor skills to use the built-in keyboard on iOS.

As I continue to tinker with the code base I will add more features and update
the read-me here.

# License
    CareDemo: A demo front end/back end using Apple's CareKit and GWT
    
    Copyright © 2016 by William Edward Woody
    
    This program is free software: you can redistribute it and/or modify 
    it under the terms of the GNU General Public License as published by 
    the Free Software Foundation, either version 3 of the License, or 
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but 
    WITHOUT ANY WARRANTY; without even the implied warranty of 
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
    General Public License for more details.
    
    You should have received a copy of the GNU General Public License 
    along with this program. If not, see http://www.gnu.org/licenses/
    
    Contact information:
    
    William Edward Woody
    12605 Raven Ridge Rd
    Raleigh, NC 27614
    United States of America
    woody@alumni.caltech.edu
    
# Why?

I believe software like CareKit and ResearchKit and other medically-related
applications represent a significant opportunity to improve the standard of
living for countless millions around the world, and I was fascinated by the
technology. This is my first crack at understanding these technologies, and
thinking through the ramifications of a more integrated care system.

If you are interested in finding out more about this code base or if you are
interested in hiring me as a consultant, please e-mail me at
woody@alumni.caltech.edu.

